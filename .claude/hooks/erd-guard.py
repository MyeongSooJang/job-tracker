#!/usr/bin/env python3
"""엔티티가 바뀌면 docs/ERD.md도 함께 고치도록 강제한다.

1. PostToolUse(Edit|Write): @Entity/@Embeddable 파일을 고치면 ERD 갱신 알림을 Claude에게 준다.
2. PreToolUse(Bash): 커밋 시 스테이징에 엔티티 변경이 있는데 docs/ERD.md가 없으면 차단한다.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ERD_PATH = "docs/ERD.md"
ENTITY_RE = re.compile(r"@(Entity|Embeddable)\b")
COMMIT_RE = re.compile(r"\bgit\b(?:\s+-[Cc]\s+\S+)*\s+commit\b")


def is_entity_file(path: Path) -> bool:
    if path.suffix != ".java" or not path.is_file():
        return False
    return ENTITY_RE.search(path.read_text(encoding="utf-8", errors="ignore")) is not None


def remind(file_path: str) -> None:
    if not is_entity_file(Path(file_path)):
        return
    message = (
        f"엔티티({Path(file_path).name})가 수정됐다. 테이블/컬럼/관계가 바뀌었다면 {ERD_PATH}도 같이 고치고, "
        "운영 DB에 반영이 필요한 DDL이 있으면 사용자에게 알려라."
    )
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": message}}))


def guard_commit() -> None:
    staged = subprocess.run(
        ["git", "diff", "--cached", "--name-only"], capture_output=True, text=True, encoding="utf-8"
    ).stdout.split()
    if ERD_PATH in staged:
        return
    changed = [name for name in staged if is_entity_file(Path(name))]
    if changed:
        sys.stderr.write(
            f"엔티티 변경({', '.join(changed)})이 스테이징됐는데 {ERD_PATH}가 없다. "
            f"테이블 구성이 바뀌었는지 확인하고 {ERD_PATH}를 고쳐 같은 커밋에 `git add` 하라. "
            "ERD에 영향이 없는 변경이면 그 이유를 사용자에게 알리고 확인받아라.\n"
        )
        sys.exit(2)


def main() -> None:
    event = json.load(sys.stdin)
    tool_input = event.get("tool_input", {})
    if event.get("hook_event_name") == "PostToolUse":
        remind(tool_input.get("file_path", ""))
    elif COMMIT_RE.search(tool_input.get("command", "")):
        guard_commit()


if __name__ == "__main__":
    main()
