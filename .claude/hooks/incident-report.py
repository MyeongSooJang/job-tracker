#!/usr/bin/env python3
"""fix 커밋을 만들 때 문제 분석 HTML(docs/incidents/)을 같은 커밋에 남기도록 한다.

PreToolUse(Bash): `git commit`의 첫 줄이 `fix(...)`이면
1. 본문에 원인/영향/놓친 이유/재발 방지가 모두 있고 항목당 150자 이내인지 검사한다. 아니면 차단(종료 코드 2)한다.
2. 커밋 메시지와 핵심 코드의 변경 전/후로 docs/incidents/<날짜>-<scope>-<해시>.html을 만들고 스테이징한다.

이미 만들어진 커밋은 CLI로 보고서를 만들 수 있다.
    python .claude/hooks/incident-report.py --from-commit <sha> [--set "원인=..."]
"""
import argparse
import hashlib
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import date
from html import escape
from pathlib import Path

INCIDENT_DIR = "docs/incidents"
FIELDS = ("원인", "영향", "놓친 이유", "재발 방지")
CORE_FILE_LABEL = "핵심 파일"
MAX_FIELD_LENGTH = 150
FIX_SUBJECT_RE = re.compile(r"^fix(?:\((?P<scope>[^)\s]+)\))?!?: (?P<desc>.+)$")
COMMIT_RE = re.compile(r"\bgit\b(?:\s+-[Cc]\s+\S+)*\s+commit\b")
STAGING_RE = re.compile(r"\bgit\b[^\n]*?\badd\b|\bcommit\b[^\n]*?\s(?:-[A-Za-z]*a[A-Za-z]*|--all)\b")
HEREDOC_RE = re.compile(
    r"<<-?\s*['\"]?(?P<tag>\w+)['\"]?[^\n]*\n(?P<body>.*?)\n[ \t]*(?P=tag)[ \t]*(?:\n|$|\))", re.S)
MESSAGE_ARG_RE = re.compile(
    r"""(?:^|\s)(?:-[A-Za-z]*m|--message)(?:=|\s+)(?:"(?P<double>(?:[^"\\]|\\.)*)"|'(?P<single>[^']*)')""", re.S)
LABEL_RE = re.compile(
    rf"^\s*[-*]?\s*(?P<label>{'|'.join(FIELDS + (CORE_FILE_LABEL,))})\s*[:：]\s*(?P<text>.*)$")
DIFF_CONTEXT_LINES = 2

GUIDE = (
    "fix 커밋에는 문제 분석이 필요하다. 커밋 본문에 아래 항목을 적어라. 신입 개발자가 읽어도 이해되게 "
    f"전문용어는 풀어 쓰고, 항목마다 1~2문장({MAX_FIELD_LENGTH}자 이내)으로 핵심만 쓴다.\n"
    "  원인: 무엇이 왜 잘못 동작했는가\n"
    "  영향: 누구에게 어떤 문제가 생기는가\n"
    "  놓친 이유: 기존 테스트나 리뷰가 왜 잡지 못했는가\n"
    "  재발 방지: 어떤 테스트·규칙·점검으로 막는가\n"
    "  핵심 파일: (선택) 변경 전/후를 보여 줄 파일 이름. 비우면 가장 많이 바뀐 파일 1개\n"
    "훅이 이 내용과 핵심 코드의 변경 전/후를 docs/incidents/*.html로 만들어 같은 커밋에 넣는다."
)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, encoding="utf-8", errors="replace"
    ).stdout


def deny(reason: str) -> None:
    sys.stderr.write(reason + "\n")
    sys.exit(2)


def extract_message(command: str):
    """커밋 명령에서 전체 메시지를 읽는다. 변수나 파일에서 오는 메시지처럼 읽을 수 없으면 None."""
    commit = COMMIT_RE.search(command)
    if not commit:
        return None
    tail = command[commit.start():]
    heredoc = HEREDOC_RE.search(tail)
    if heredoc:
        return heredoc.group("body")
    parts = [
        (match.group("double") or match.group("single") or "").replace('\\"', '"')
        for match in MESSAGE_ARG_RE.finditer(tail)
    ]
    return "\n\n".join(parts) if parts else None


def parse_fields(message: str) -> dict:
    """`원인: ...` 형태의 항목과, 다음 항목이나 빈 줄 전까지 이어지는 줄을 읽는다."""
    fields, current = {}, None
    for line in message.splitlines()[1:]:
        label = LABEL_RE.match(line)
        if label:
            current = label.group("label")
            fields[current] = label.group("text").strip()
        elif current and line.strip():
            fields[current] = (fields[current] + "\n" + line.strip().lstrip("-* ").strip()).strip()
        else:
            current = None
    return {name: text for name, text in fields.items() if text}


@dataclass
class FileDiff:
    path: str
    hunks: list = field(default_factory=list)  # [(헤더, [(' ' | '-' | '+', 줄)])]

    @property
    def is_test(self) -> bool:
        return "/test/" in f"/{self.path}"

    @property
    def is_code(self) -> bool:
        return self.path.endswith((".java", ".kt", ".py", ".js", ".ts", ".tsx", ".yml", ".yaml", ".sql"))

    @property
    def changed_lines(self) -> int:
        return sum(1 for _, rows in self.hunks for mark, _ in rows if mark != " ")


def parse_diff(text: str) -> list:
    files, current, rows = [], None, None
    for line in text.splitlines():
        if line.startswith("diff --git"):
            current, rows = None, None
        elif rows is None and line.startswith("--- "):
            current = FileDiff(line[4:].removeprefix("a/"))
        elif rows is None and line.startswith("+++ "):
            path = line[4:].removeprefix("b/")
            if path != "/dev/null":
                current = FileDiff(path)
        elif line.startswith("@@"):
            if current is None:
                continue
            if current not in files:
                files.append(current)
            rows = []
            current.hunks.append((line, rows))
        elif rows is not None and line[:1] in (" ", "+", "-"):
            rows.append((line[0], line[1:]))
    return [f for f in files if f.path and not f.path.startswith(INCIDENT_DIR)]


def split_core(code_files: list, fields: dict):
    """핵심 파일(지정하거나, 지정이 없으면 가장 많이 바뀐 파일 1개)과 나머지로 나눈다."""
    named = [name.strip() for name in fields.get(CORE_FILE_LABEL, "").replace("\n", ",").split(",") if name.strip()]
    core = [f for f in code_files if any(f.path.endswith(name) for name in named)]
    if not core and code_files:
        core = [max(code_files, key=lambda f: f.changed_lines)]
    return core, [f for f in code_files if f not in core]


def staged_diff() -> str:
    return git("diff", "--cached", f"-U{DIFF_CONTEXT_LINES}", "--no-color", "--no-ext-diff")


def worktree_diff() -> str:
    """같은 명령에서 `git add`가 이어질 때는 아직 스테이징 전이므로 작업 트리 전체를 읽는다."""
    text = git("diff", "HEAD", f"-U{DIFF_CONTEXT_LINES}", "--no-color", "--no-ext-diff")
    for name in git("ls-files", "--others", "--exclude-standard").splitlines():
        text += git("diff", "--no-index", f"-U{DIFF_CONTEXT_LINES}", "--no-color", "--", "/dev/null", name)
    return text


def render_lines(rows, kind: str) -> str:
    classes = {"-": "del", "+": "add", " ": "ctx"}
    return "".join(
        f'<span class="l {classes[mark]}">{escape(text) or "&nbsp;"}</span>'
        for mark, text in rows if mark in (" ", kind)
    )


def render_file(file_diff: FileDiff) -> str:
    hunks = "".join(
        f'<div class="hunk"><div class="hunk-head">{escape(header.split("@@")[-1].strip() or header)}</div>'
        f'<div class="cols">'
        f'<div><div class="col-title">변경 전</div><pre><code>{render_lines(rows, "-")}</code></pre></div>'
        f'<div><div class="col-title">변경 후</div><pre><code>{render_lines(rows, "+")}</code></pre></div>'
        f'</div></div>'
        for header, rows in file_diff.hunks
    )
    name = file_diff.path.rsplit("/", 1)[-1]
    return f'<section class="file"><h3>{escape(name)}<small>{escape(file_diff.path)}</small></h3>{hunks}</section>'


def render_text(text: str) -> str:
    return escape(text).replace("\n", "<br>")


STYLE = """
:root{--bg:#fbfaf8;--surface:#fff;--ink:#1d1d1f;--muted:#6b6b70;--line:#e4e1db;--accent:#b4332b;
--del:#fde8e6;--del-ink:#8c1d18;--add:#e5f4e8;--add-ink:#1e6b34;--ctx:#f3f1ed}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#161517;--surface:#201f22;--ink:#ececee;
--muted:#9b9ba1;--line:#35333a;--accent:#ff8a80;--del:#3a1f1e;--del-ink:#ffb4ab;--add:#1c3324;--add-ink:#8fdba4;--ctx:#262529}}
:root[data-theme="dark"]{--bg:#161517;--surface:#201f22;--ink:#ececee;--muted:#9b9ba1;--line:#35333a;--accent:#ff8a80;
--del:#3a1f1e;--del-ink:#ffb4ab;--add:#1c3324;--add-ink:#8fdba4;--ctx:#262529}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);
font:16px/1.6 -apple-system,"Segoe UI","Apple SD Gothic Neo","Malgun Gothic",sans-serif}
main{max-width:1040px;margin:0 auto;padding:32px 16px 64px}
.badge{display:inline-block;font-size:12px;font-weight:700;letter-spacing:.08em;color:var(--accent);
border:1px solid var(--accent);border-radius:4px;padding:1px 8px}
h1{font-size:28px;line-height:1.3;margin:12px 0 8px}h2{font-size:20px;margin:40px 0 12px}
h3{font-size:15px;margin:0 0 8px;word-break:break-all}
h3 small{display:block;font-weight:400;color:var(--muted);font-size:12px;font-family:ui-monospace,Consolas,monospace}
.meta{color:var(--muted);font-size:14px;display:flex;flex-wrap:wrap;gap:4px 20px}
.cards{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
.card{background:var(--surface);border:1px solid var(--line);border-radius:8px;padding:14px 16px}
.card h2{font-size:14px;margin:0 0 6px;color:var(--accent)}.card p{margin:0}
.file{background:var(--surface);border:1px solid var(--line);border-radius:8px;padding:16px;margin-bottom:16px;min-width:0}
.hunk{margin-top:12px}.hunk-head{font-size:12px;color:var(--muted);font-family:ui-monospace,Consolas,monospace;margin-bottom:4px}
.cols{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.cols>div{min-width:0}
.col-title{font-size:12px;font-weight:700;color:var(--muted);margin-bottom:2px}
pre{margin:0;overflow-x:auto;border:1px solid var(--line);border-radius:6px;background:var(--ctx);
font:13px/1.5 ui-monospace,Consolas,monospace}code{display:block;min-width:max-content}
.l{display:block;padding:0 10px;white-space:pre}.l.del{background:var(--del);color:var(--del-ink)}
.l.add{background:var(--add);color:var(--add-ink)}details{margin-top:8px}summary{cursor:pointer;color:var(--muted)}
ul{padding-left:20px}.empty{color:var(--muted)}
@media (max-width:760px){.cards,.cols{grid-template-columns:minmax(0,1fr)}h1{font-size:23px}}
"""


def build_html(subject: str, fields: dict, files: list, meta: dict) -> str:
    desc = FIX_SUBJECT_RE.match(subject).group("desc")
    cards = "".join(
        f'<div class="card"><h2>{name}</h2><p>{render_text(fields.get(name, "(기록 없음)"))}</p></div>'
        for name in FIELDS
    )
    code = [f for f in files if f.is_code and not f.is_test]
    tests = [f for f in files if f.is_test]
    others = [f.path for f in files if not f.is_code and not f.is_test]
    core, rest = split_core(code, fields)
    code_html = "".join(map(render_file, core)) or '<p class="empty">변경된 운영 코드가 없습니다.</p>'
    if rest:
        names = "".join(f"<li>{escape(f.path.rsplit('/', 1)[-1])}</li>" for f in rest)
        code_html += (f'<details><summary>그 외 변경된 코드 {len(rest)}개 파일 (이름 변경 등 부수적 수정)</summary>'
                      f'<ul>{names}</ul></details>')
    tests_html = (
        f'<details><summary>회귀 테스트 변경 {len(tests)}개 파일 보기</summary>{"".join(map(render_file, tests))}</details>'
        if tests else '<p class="empty">추가·변경된 테스트가 없습니다.</p>'
    )
    others_html = f'<h2>기타 변경 파일</h2><ul>{"".join(f"<li>{escape(p)}</li>" for p in others)}</ul>' if others else ""
    meta_html = "".join(f"<span>{escape(label)}: {escape(value)}</span>" for label, value in meta.items())
    return (
        f'<!doctype html><html lang="ko"><head><meta charset="utf-8">'
        f'<meta name="viewport" content="width=device-width,initial-scale=1">'
        f'<title>문제 분석 {escape(desc)}</title><style>{STYLE}</style></head><body><main>'
        f'<span class="badge">FIX</span><h1>{escape(desc)}</h1><div class="meta">{meta_html}</div>'
        f'<h2>문제 분석</h2><div class="cards">{cards}</div>'
        f'<h2>핵심 코드 변경 전/후</h2>{code_html}<h2>회귀 테스트</h2>{tests_html}{others_html}'
        f'</main></body></html>'
    )


def write_report(subject: str, fields: dict, files: list, meta: dict, report_date: str) -> str:
    scope = FIX_SUBJECT_RE.match(subject).group("scope") or "fix"
    digest = hashlib.sha1(subject.encode("utf-8")).hexdigest()[:6]
    path = Path(INCIDENT_DIR) / f"{report_date}-{scope}-{digest}.html"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(build_html(subject, fields, files, meta), encoding="utf-8")
    return path.as_posix()


def run_hook() -> None:
    event = json.load(sys.stdin)
    command = event.get("tool_input", {}).get("command", "")
    merging = subprocess.run(["git", "rev-parse", "-q", "--verify", "MERGE_HEAD"], capture_output=True).returncode == 0
    message = None if merging or not COMMIT_RE.search(command) else extract_message(command)
    subject = message.strip().splitlines()[0].strip() if message and message.strip() else ""
    if not FIX_SUBJECT_RE.match(subject):
        return
    fields = parse_fields(message)
    missing = [name for name in FIELDS if name not in fields]
    if missing:
        deny(f"fix 커밋 본문에 {', '.join(missing)} 항목이 없다.\n{GUIDE}")
    too_long = [name for name in FIELDS if len(fields[name]) > MAX_FIELD_LENGTH]
    if too_long:
        deny(f"{', '.join(too_long)} 항목이 {MAX_FIELD_LENGTH}자를 넘는다. 핵심만 1~2문장으로 줄여라.\n{GUIDE}")
    diff = worktree_diff() if STAGING_RE.search(command) else staged_diff()
    meta = {"날짜": date.today().isoformat(), "브랜치": git("branch", "--show-current").strip(), "커밋": "커밋 전 생성"}
    path = write_report(subject, fields, parse_diff(diff), meta, date.today().isoformat())
    git("add", path)
    note = f"문제 분석 보고서 {path}를 만들어 스테이징했다. 같은 커밋에 포함된다."
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext": note}}, ensure_ascii=False))


def run_cli(args) -> None:
    sha = args.from_commit
    message = git("show", "-s", "--format=%B", sha).strip()
    subject = message.splitlines()[0] if message else ""
    if not FIX_SUBJECT_RE.match(subject):
        sys.exit(f"{sha}는 fix 커밋이 아니다: {subject!r}")
    fields = parse_fields(message)
    for item in args.set:
        name, _, text = item.partition("=")
        fields[name.strip()] = text.strip()
    commit_date = git("show", "-s", "--format=%cs", sha).strip()
    meta = {"날짜": commit_date, "커밋": git("rev-parse", "--short", sha).strip()}
    diff = git("diff", f"-U{DIFF_CONTEXT_LINES}", "--no-color", "--no-ext-diff", f"{sha}^", sha)
    print(write_report(subject, fields, parse_diff(diff), meta, commit_date))


def main() -> None:
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--from-commit")
    parser.add_argument("--set", action="append", default=[], help="항목=내용 (예: 원인=...)")
    args = parser.parse_args()
    run_cli(args) if args.from_commit else run_hook()


if __name__ == "__main__":
    main()
