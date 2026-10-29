# Task 4 Fix Report — Interview Domain

**Date:** 2026-10-26
**Commit:** 50a1b7b

## Fixes Applied

### Fix 1: DTO 레이어 분리
- `InterviewRequest`, `InterviewQuestionRequest`를 `presentation/controller/dto/`에서 `application/service/dto/`로 이동
- `InterviewService`, `InterviewController`의 import 경로 업데이트
- 기존 파일 삭제 (git rename으로 추적)

### Fix 2: addQuestion() 소유권 검사 추가
- `InterviewService.addQuestion()`에서 인터뷰 조회 후 `interview.getApplication().getId().equals(applicationId)` 검사 추가
- 불일치 시 `BusinessException(ErrorCode.INTERVIEW_NOT_FOUND)` 발생

### Minor Fix: @NotNull → @Min(1)
- `InterviewRequest.round` 제약을 `@NotNull`에서 `@Min(1)`으로 변경
- `jakarta.validation.constraints.Min` import 추가

## Test Results
- 총 19개 테스트 모두 통과 (BUILD SUCCESSFUL)
