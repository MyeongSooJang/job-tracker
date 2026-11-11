# 훅 작업 가이드

`.claude/hooks/` 아래 파일을 수정하거나 훅 동작을 검증할 때만 필요한 내용이다. 루트 `CLAUDE.md`에는 차단 규칙 요약만 둔다.

## 동작 방식

- `/security-review` 직후 `mark-skill.cjs`가 "취약점이 없으면 묻지 말고 바로 push·PR, 있으면 멈추고 보고, 머지는 지시가 있을 때만"을 additionalContext로 상기시킨다. 훅은 리뷰 결과를 알 수 없어 강제가 아니라 안내다.
- 스킬 실행 기록은 `mark-skill.cjs`(PostToolUse)가 `.claude/.state/`(git 제외)에 남긴다. 스킬을 **호출한** 사실만 기록하며, 리뷰 결과를 반영했는지는 확인하지 못한다.
- 훅은 명령 문자열에서 heredoc·따옴표 속 글을 제외하고 판단하며, `cd 경로 &&`로 다른 저장소에서 실행하는 명령에는 적용하지 않는다. 사용자가 직접 입력하는 `! 명령`은 훅을 거치지 않는다.
- 훅은 세션을 연 저장소의 설정이 `/add-dir`로 추가한 다른 저장소(프론트엔드) 작업에도 실행된다. 파이썬 훅(`erd-guard.py`, `incident-report.py`)은 `scope.py`로 작업 대상이 이 저장소 밖이면 건너뛴다. 훅 명령은 상대 경로가 아니라 `$CLAUDE_PROJECT_DIR`로 쓴다.

## 수정과 검증

- 훅 동작 검증: `node --test .claude/hooks/guard-check.cjs` (임시 git 저장소에 스크립트를 복사해 실행한다). 훅을 고치면 함께 돌린다.
- 파이썬 훅 테스트: `python -m unittest discover -s .claude/hooks -p "test_*.py"`.
- 같은 스크립트가 프론트엔드 저장소에도 있다(`guard.config.json`만 다르다). 한쪽을 고치면 다른 쪽도 같이 고친다.
- 새 훅 설정은 이미 열려 있는 세션에서는 `/hooks`를 한 번 열거나 세션을 다시 시작해야 적용될 수 있다.

## fix 커밋의 사고 보고서

- `fix` 커밋 본문에는 `원인:`, `영향:`, `놓친 이유:`, `재발 방지:`를 항목당 150자 이내로 적는다. 선택으로 `핵심 파일:`에 변경 전/후를 보여 줄 파일을 지정한다.
- `incident-report.py`가 항목이 없으면 커밋을 차단하고, 있으면 핵심 코드의 변경 전/후가 담긴 `docs/incidents/*.html`을 로컬에 만든다(추적하지 않아 커밋에는 들어가지 않는다).
- 이미 만든 커밋은 `python .claude/hooks/incident-report.py --from-commit <sha> --set "원인=..."`으로 만든다.
