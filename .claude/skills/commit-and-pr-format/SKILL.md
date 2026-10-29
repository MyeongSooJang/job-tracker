---
name: commit-and-pr-format
description: Use when creating git commits, branches, or pull requests ("커밋해줘", "PR 만들어줘", "브랜치 따줘", "머지해줘"), or when instructing a coding agent to commit or open a PR after writing or refactoring code. Defines Conventional Commits 기반 커밋 양식, 브랜치 이름, PR 양식, 머지 절차.
---

# Commit & PR Format

Conventional Commits 기반. 모든 메시지와 PR 본문은 한국어로 작성한다.

## 작업 흐름

```
main에서 브랜치 생성 → 커밋 → PR 생성 → 리뷰/검증 → main에 머지 → 브랜치 삭제
```

- 새 기능/수정은 항상 `main`에서 브랜치를 따서 시작한다. `main`에 직접 커밋하지 않는다.
- 한 브랜치 = 한 목적. PR이 커지면(대략 400줄 초과) 쪼갤 수 있는지 먼저 검토한다.
- 머지는 **squash merge**를 기본으로 한다. (main 히스토리가 PR 단위로 정리됨. 다른 방식이 필요하면 사용자가 지시한다.)
- 머지 후 작업 브랜치를 삭제한다.

## 브랜치 이름

```
<type>/<짧은-설명-kebab-case>
```

- type 접두어: `feature`(feat), `fix`, `refactor`, `chore`, `docs`, `test`, `perf`
- 설명은 영문 소문자와 하이픈 (예: `feature/interview-question-api`, `fix/lazy-loading-error`)
- 이슈가 있으면 번호를 붙인다 (예: `fix/12-cors-error`)

---

## 커밋 양식

### 구조
```
type(scope): 한국어 설명

body (선택)

footer (선택)
```

### type — 아래 중 하나만 사용
| type | 사용 상황 |
|------|-----------|
| feat | 새 기능 추가 |
| fix | 버그 수정 |
| refactor | 동작 변경 없는 코드 개선 |
| perf | 성능 개선 |
| test | 테스트 추가/수정 |
| docs | 문서 변경 |
| style | 포맷팅 등 동작과 무관한 변경 (공백, 세미콜론) |
| build | 빌드 설정, 의존성 변경 |
| ci | CI/CD 설정 변경 |
| chore | 위에 해당하지 않는 잡무 (.gitignore 등) |
| revert | 이전 커밋 되돌리기 |

### scope — 선택
- 변경 대상 모듈/도메인이 뚜렷할 때만 작성한다 (예: `order`, `auth`, `schedule`).
- 쓰기로 했다면 프로젝트 안에서 같은 이름을 일관되게 쓴다.
- 전체에 걸친 변경이거나 모호하면 생략한다.

### subject (첫 줄)
- 50자 이내. 마침표를 붙이지 않는다.
- 명사형으로 끝낸다 (예: "~추가", "~수정", "~제거").
- "무엇을 했는지"가 한눈에 보이게 쓴다.

### body
- **무엇보다 왜**를 쓴다. 코드를 보면 알 수 있는 "무엇"은 반복하지 않는다.
- subject로 충분한 사소한 변경은 body를 생략한다.
- 쓸 때는 빈 줄로 subject와 구분하고, 변경 내용은 bullet로 정리한다.
- fix/refactor는 첫 줄에 문제(원인)를 한 줄로 정의한 뒤 bullet로 변경 내용을 쓴다.

### footer
- 허용: 이슈 연결 (`Closes #12`, `Refs #7`), `BREAKING CHANGE: 설명`
- 금지: `Co-Authored-By`, `Generated with Claude Code` 등 AI 서명류

### Breaking Change
하위 호환이 깨지면 type/scope 뒤에 `!`를 붙이고 footer에 설명한다.
```
feat(api)!: 지원 현황 응답 필드명 변경

BREAKING CHANGE: status 필드가 state로 변경됨
```

### 커밋 단위
- 하나의 논리적 변경 = 하나의 커밋. 서로 다른 목적(기능 + 포맷팅 등)을 섞지 않는다.
- 각 커밋은 빌드와 테스트가 깨지지 않는 상태로 남긴다.

### 예시

**feat**
```
feat(order): 주문 할인 정책 추가

주문 금액에 따라 할인율이 달라지는 요구사항 대응.
- DiscountPolicy 인터페이스 및 RateDiscountPolicy 구현
- OrderService에 정책 적용 로직 연결
```

**fix**
```
fix(auth): 로그인 실패 시 토큰이 초기화되지 않는 버그 수정

JwtFilter 예외 처리 블록에서 SecurityContext 초기화 누락.
- doFilterInternal 예외 경로에 SecurityContextHolder.clearContext() 추가
- 인증 실패 응답 코드 401로 통일

Closes #12
```

**refactor**
```
refactor(ai): KafkaProducerConfig 제거 및 common-messaging 모듈로 통합

spring-boot-starter-kafka가 ai 모듈에 중복 선언되어 설정 충돌 발생.
- ai 모듈에서 spring-boot-starter-kafka 의존성 제거
- KafkaProducerConfig 클래스 삭제
- common-messaging 의존성 추가
```

**body 생략 (사소한 변경)**
```
fix: CORS에 Vercel 도메인 추가
```

---

## PR 양식

### 제목
커밋과 같은 형식 `type(scope): 한국어 설명`을 쓴다. squash merge 시 이 제목이 main의 커밋 메시지가 된다.

### 본문
아래 섹션을 순서대로 포함한다. 해당 없는 섹션은 "해당 없음"으로 적거나 생략한다.

```markdown
## 관련 이슈
<!-- Closes #번호. 없으면 생략 -->

## 변경 사항
<!-- 무엇이 바뀌었는지 bullet으로 -->

## 변경 이유
<!-- 왜 이 변경이 필요했는지, 어떤 문제를 해결하는지 -->

## 리뷰 포인트
<!-- 리뷰어가 집중해서 봐야 할 부분, 고민했던 설계 결정 -->

## 테스트 계획
- [ ] 자동화 테스트: (실행한 테스트와 결과)
- [ ] 수동 확인: (시나리오 설명)

## 배포 영향
<!-- DB 마이그레이션, 환경변수 추가, 롤백 방법 등. 없으면 "없음" -->

## 스크린샷
<!-- UI/API 응답 변경 시. 없으면 생략 -->

## 체크리스트
- [ ] Breaking Change 없음 (있다면 위 변경 사항에 설명)
```

### 작성 규칙
- 본문에 `🤖 Generated with Claude Code` 등 AI 서명을 넣지 않는다.
- "코드 리뷰 후속 작업" 체크리스트 섹션을 넣지 않는다.
- 테스트 결과는 실제로 실행한 것만 체크한다. 실행하지 않은 항목을 체크하지 않는다.

### 머지 전 확인
1. PR이 `main` 대상이고 충돌이 없는가 (`mergeable`)
2. 테스트 계획의 항목이 실제로 확인되었는가
3. CI 체크가 설정되어 있다면 통과했는가
