"""incident-report 훅 테스트. 실행: python -m unittest discover -s .claude/hooks -p "test_*.py" """
import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HOOK = Path(__file__).with_name("incident-report.py")
spec = importlib.util.spec_from_file_location("incident_report", HOOK)
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)

FIX_MESSAGE = """fix(auth): 인증 코드 시도 제한 우회 수정

원인: 읽고 비교한 뒤에 횟수를 셌다
영향: 6자리 코드를 추측할 수 있다
놓친 이유: 순차 테스트만 있었다
- 동시 요청은 재현하지 못했다
재발 방지: 원자 연산을 먼저 실행한다"""


class ExtractMessageTest(unittest.TestCase):
    def test_파일_입력_heredoc에서_전체_메시지를_읽는다(self):
        command = "cd x && git commit -q -F - <<'EOF'\nfix: 제목\n\n본문\nEOF\ngit log"
        self.assertEqual(report.extract_message(command), "fix: 제목\n\n본문")

    def test_cat_heredoc_치환에서_전체_메시지를_읽는다(self):
        command = "git commit -m \"$(cat <<'EOF'\nfix: 제목\n\n본문\nEOF\n)\""
        self.assertEqual(report.extract_message(command), "fix: 제목\n\n본문")

    def test_여러_m_옵션은_빈_줄로_이어_붙인다(self):
        self.assertEqual(report.extract_message('git commit -m "fix: 제목" -m "본문"'), "fix: 제목\n\n본문")

    def test_메시지를_읽을_수_없으면_None이다(self):
        self.assertIsNone(report.extract_message("git commit --amend --no-edit"))


class ParseFieldsTest(unittest.TestCase):
    def test_네_항목을_읽고_이어지는_줄을_합친다(self):
        fields = report.parse_fields(FIX_MESSAGE)

        self.assertEqual(set(fields), set(report.FIELDS))
        self.assertEqual(fields["놓친 이유"], "순차 테스트만 있었다\n동시 요청은 재현하지 못했다")

    def test_빠진_항목은_포함하지_않는다(self):
        self.assertNotIn("영향", report.parse_fields("fix: 제목\n\n원인: 이유"))


class ParseDiffTest(unittest.TestCase):
    MODIFIED = (
        "diff --git a/A.java b/A.java\n--- a/A.java\n+++ b/A.java\n@@ -1,2 +1,2 @@ class A\n"
        " keep\n-old\n+new\n+++ looks like header\n"
    )

    def test_수정된_파일의_변경_전후_줄을_나눈다(self):
        files = report.parse_diff(self.MODIFIED)

        self.assertEqual([f.path for f in files], ["A.java"])
        rows = files[0].hunks[0][1]
        self.assertIn(("-", "old"), rows)
        self.assertIn(("+", "new"), rows)
        self.assertIn(("+", "++ looks like header"), rows)

    def test_삭제된_파일도_경로가_남는다(self):
        diff = "diff --git a/B.java b/B.java\n--- a/B.java\n+++ /dev/null\n@@ -1 +0,0 @@\n-gone\n"

        self.assertEqual([f.path for f in report.parse_diff(diff)], ["B.java"])

    def test_보고서_자신은_제외한다(self):
        diff = f"diff --git a/docs/incidents/r.html b/docs/incidents/r.html\n--- /dev/null\n+++ b/docs/incidents/r.html\n@@ -0,0 +1 @@\n+x\n"

        self.assertEqual(report.parse_diff(diff), [])


def code_file(path, changed):
    return report.FileDiff(path, [("@@", [("+", "x")] * changed)])


class SplitCoreTest(unittest.TestCase):
    def test_지정이_없으면_가장_많이_바뀐_파일_하나만_핵심이다(self):
        small, big = code_file("a/Small.java", 1), code_file("a/Big.java", 5)

        core, rest = report.split_core([small, big], {})

        self.assertEqual((core, rest), ([big], [small]))

    def test_핵심_파일을_이름으로_지정하면_그_파일이_핵심이다(self):
        small, big = code_file("a/Small.java", 1), code_file("a/Big.java", 5)

        core, rest = report.split_core([small, big], {"핵심 파일": "Small.java"})

        self.assertEqual((core, rest), ([small], [big]))

    def test_코드가_없으면_핵심도_없다(self):
        self.assertEqual(report.split_core([], {}), ([], []))


class HookRunTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        for args in (["init", "-q"], ["config", "user.email", "t@example.com"], ["config", "user.name", "t"]):
            self.git(*args)
        (self.repo / "A.java").write_text("class A {\n  int v = 1;\n}\n", encoding="utf-8")
        self.git("add", "-A")
        self.git("commit", "-q", "-m", "init")
        (self.repo / "A.java").write_text("class A {\n  int v = 2;\n}\n", encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, capture_output=True, text=True, encoding="utf-8")

    def run_hook(self, command):
        payload = json.dumps({"hook_event_name": "PreToolUse", "tool_input": {"command": command}})
        env = {**os.environ, "CLAUDE_PROJECT_DIR": str(self.repo)}
        return subprocess.run(
            [sys.executable, str(HOOK)], cwd=self.repo, input=payload, capture_output=True, text=True,
            encoding="utf-8", env=env)

    def test_fix가_아닌_커밋은_아무것도_하지_않는다(self):
        result = self.run_hook("git commit -m 'feat: 기능 추가'")

        self.assertEqual((result.returncode, result.stdout), (0, ""))
        self.assertFalse((self.repo / "docs/incidents").exists())

    def test_fix_커밋에_분석_항목이_없으면_차단한다(self):
        result = self.run_hook("git commit -m 'fix: 버그 수정'")

        self.assertEqual(result.returncode, 2)
        self.assertIn("원인", result.stderr)

    def test_항목이_150자를_넘으면_차단한다(self):
        long_message = FIX_MESSAGE.replace("원인: 읽고 비교한 뒤에 횟수를 셌다", "원인: " + "가" * 151)

        result = self.run_hook(f"git commit -q -F - <<'EOF'\n{long_message}\nEOF")

        self.assertEqual(result.returncode, 2)
        self.assertIn("150자", result.stderr)

    def test_add와_커밋을_한_명령으로_실행해도_변경_전후를_담은_보고서를_만들고_스테이징하지_않는다(self):
        command = f"git add -A && git commit -q -F - <<'EOF'\n{FIX_MESSAGE}\nEOF"

        result = self.run_hook(command)

        self.assertEqual(result.returncode, 0, result.stderr)
        reports = list((self.repo / "docs/incidents").glob("*-auth-*.html"))
        self.assertEqual(len(reports), 1)
        html = reports[0].read_text(encoding="utf-8")
        self.assertIn("int v = 1;", html)
        self.assertIn("int v = 2;", html)
        self.assertIn("순차 테스트만 있었다", html)
        self.assertNotIn("docs/incidents", self.git("diff", "--cached", "--name-only").stdout)


if __name__ == "__main__":
    unittest.main()
