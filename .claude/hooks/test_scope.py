"""훅 범위(scope) 테스트. 실행: python -m unittest discover -s .claude/hooks -p "test_*.py" """
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HOOKS = Path(__file__).resolve().parent
sys.path.insert(0, str(HOOKS))
from scope import in_project  # noqa: E402

FIX_WITHOUT_FIELDS = "git commit -m 'fix: 버그 수정'"


class InProjectTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.backend = Path(self.tmp.name, "backend")
        self.frontend = Path(self.tmp.name, "frontend")
        self.backend.mkdir()
        self.frontend.mkdir()
        self.saved = os.environ.get("CLAUDE_PROJECT_DIR")
        os.environ["CLAUDE_PROJECT_DIR"] = str(self.backend)

    def tearDown(self):
        if self.saved is None:
            os.environ.pop("CLAUDE_PROJECT_DIR", None)
        else:
            os.environ["CLAUDE_PROJECT_DIR"] = self.saved
        self.tmp.cleanup()

    def test_저장소_안에서_실행한_명령은_대상이다(self):
        self.assertTrue(in_project({"cwd": str(self.backend / "src")}))

    def test_다른_저장소에서_실행한_명령은_대상이_아니다(self):
        self.assertFalse(in_project({"cwd": str(self.frontend)}))

    def test_저장소_안의_파일_수정은_대상이다(self):
        self.assertTrue(in_project({"tool_input": {"file_path": str(self.backend / "A.java")}}))

    def test_다른_저장소의_파일_수정은_대상이_아니다(self):
        self.assertFalse(in_project({"tool_input": {"file_path": str(self.frontend / "A.java")}}))

    def test_이름이_비슷한_형제_폴더는_대상이_아니다(self):
        sibling = Path(self.tmp.name, "backend-extra")
        sibling.mkdir()

        self.assertFalse(in_project({"cwd": str(sibling)}))

    def test_기준_저장소를_알_수_없으면_모두_대상이다(self):
        del os.environ["CLAUDE_PROJECT_DIR"]

        self.assertTrue(in_project({"cwd": str(self.frontend)}))


class HookScopeTest(unittest.TestCase):
    """다른 저장소(프론트엔드)에서 실행해도 훅이 오류도 차단도 하지 않는다."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.backend = Path(self.tmp.name, "backend")
        self.frontend = Path(self.tmp.name, "frontend")
        self.backend.mkdir()
        self.frontend.mkdir()
        subprocess.run(["git", "init", "-q"], cwd=self.frontend, check=True)
        (self.frontend / "Entity.java").write_text("@Entity\nclass Entity {}\n", encoding="utf-8")
        subprocess.run(["git", "add", "-A"], cwd=self.frontend, check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def run_hook(self, script, event):
        env = {**os.environ, "CLAUDE_PROJECT_DIR": str(self.backend)}
        return subprocess.run(
            [sys.executable, str(HOOKS / script)], cwd=self.frontend, input=json.dumps(event),
            capture_output=True, text=True, encoding="utf-8", env=env)

    def bash_event(self):
        return {"hook_event_name": "PreToolUse", "cwd": str(self.frontend),
                "tool_input": {"command": FIX_WITHOUT_FIELDS}}

    def test_다른_저장소의_fix_커밋은_분석_항목이_없어도_막지_않는다(self):
        result = self.run_hook("incident-report.py", self.bash_event())

        self.assertEqual((result.returncode, result.stderr), (0, ""))

    def test_다른_저장소의_엔티티_커밋은_ERD_없이도_막지_않는다(self):
        event = {**self.bash_event(), "tool_input": {"command": "git commit -m 'feat: 추가'"}}

        result = self.run_hook("erd-guard.py", event)

        self.assertEqual((result.returncode, result.stderr), (0, ""))

    def test_다른_저장소의_엔티티_파일_수정은_ERD_알림을_내지_않는다(self):
        event = {"hook_event_name": "PostToolUse", "cwd": str(self.frontend),
                 "tool_input": {"file_path": str(self.frontend / "Entity.java")}}

        result = self.run_hook("erd-guard.py", event)

        self.assertEqual((result.returncode, result.stdout), (0, ""))

    def test_이_저장소의_엔티티_파일_수정은_ERD_알림을_낸다(self):
        entity = self.backend / "Entity.java"
        entity.write_text("@Entity\nclass Entity {}\n", encoding="utf-8")
        event = {"hook_event_name": "PostToolUse", "cwd": str(self.backend),
                 "tool_input": {"file_path": str(entity)}}

        result = self.run_hook("erd-guard.py", event)

        self.assertIn("ERD", result.stdout)


if __name__ == "__main__":
    unittest.main()
