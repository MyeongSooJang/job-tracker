"""훅이 이 저장소의 작업에만 반응하도록 범위를 판단한다.

세션을 연 저장소(CLAUDE_PROJECT_DIR)의 훅은 /add-dir로 추가한 다른 저장소(예: 프론트엔드)의
작업에도 실행된다. 그 저장소에는 엔티티도 `fix` 커밋 규칙도 없으므로 건너뛰어야 한다.
"""
import os
from pathlib import Path


def _is_under(path: str, root: str) -> bool:
    try:
        Path(os.path.realpath(path)).relative_to(os.path.realpath(root))
    except ValueError:
        return False
    return True


def in_project(event: dict) -> bool:
    """훅 입력의 작업 대상(수정한 파일 또는 명령을 실행하는 폴더)이 이 저장소 안인가."""
    project = os.environ.get("CLAUDE_PROJECT_DIR")
    if not project:
        return True  # 기준을 알 수 없으면 지금까지처럼 모두 검사한다
    target = event.get("tool_input", {}).get("file_path") or event.get("cwd") or os.getcwd()
    return _is_under(target, project)
