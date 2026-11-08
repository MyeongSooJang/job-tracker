# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

취업 활동(서류 제출 ~ 면접) 기록 관리용 Spring Boot 3.3 / Java 21 백엔드 (Gradle, JPA, PostgreSQL). 프론트엔드는 별도 저장소(Vercel 배포)이며, 설계 문서는 `docs/superpowers/specs/`, 구현 계획은 `docs/superpowers/plans/`에 두지만 git에서 추적하지 않는다(로컬 전용).

## 명령어

```bash
./gradlew bootRun                                   # 로컬 PostgreSQL 필요 (아래 환경변수 참고)
./gradlew build -x test                             # 빌드 (Dockerfile과 동일)
./gradlew test                                      # 전체 테스트
./gradlew test --tests com.jobtracker.domain.ApplicationTest
./gradlew test --tests '*ApplicationControllerTest.POST_applications*'   # 단일 테스트
```

```bash
./gradlew test jacocoTestReport                     # 테스트 + 커버리지 리포트 (build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestCoverageVerification            # 라인 커버리지 70% 유지선 확인 (테스트 선행)
./gradlew pitest                                    # 도메인 패키지 뮤테이션 테스트 (build/reports/pitest/index.html)
```

- Swagger UI: `/swagger-ui.html`
- DB 설정은 `application.yml`의 환경변수 사용: `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`/`DB_USERNAME`, `PGPASSWORD`/`DB_PASSWORD`, `PORT` (Railway/Render 배포 기준). `ddl-auto: update`이며 마이그레이션 도구는 없다.
- 이메일 인증은 Redis와 Gmail API(HTTPS)를 쓴다. Render 무료 플랜이 SMTP 포트(25/465/587)를 막아서 SMTP는 쓰지 않는다: `REDIS_URL`(기본 `redis://localhost:6379`), `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`(`gmail.send` 범위, 동의 화면은 프로덕션 게시 상태여야 토큰이 만료되지 않는다), `MAIL_FROM`(토큰을 발급한 Gmail 주소). 모두 기본값이 없어 미설정 시 기동에 실패한다. 로컬 Redis는 `docker run -d -p 6379:6379 redis:7`.
- 테스트 메서드명은 한글(`POST_applications_지원_건을_등록한다`) 스타일을 따른다.

## 아키텍처

패키지는 `com.jobtracker` 아래 계층형 + DDD 풍으로 나뉜다.

- `presentation/controller` — REST 컨트롤러 (`/api/**`)
- `application/service` — 트랜잭션 서비스 (클래스 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`), 요청 DTO는 `service/dto`
- `domain/<aggregate>` — 엔티티, enum, **Repository 인터페이스**. 비즈니스 규칙은 엔티티/enum이 담당 (예: `ApplicationStatus.canAdvanceTo`로 상태 전이 검증)
- `infrastructure/persistence` — `XxxJpaRepository`가 `JpaRepository`와 도메인 `XxxRepository` 인터페이스를 **함께 상속**하는 구조 (별도 어댑터 구현 없음)
- `global` — `CorsConfig`, `JpaConfig`(`@EnableJpaAuditing`), `BusinessException` + `ErrorCode` + `GlobalExceptionHandler`

도메인 관계: `Company` → `Application` → (`Schedule`, `Interview` → `InterviewQuestion`). 엔티티는 정적 팩토리 `create(...)`로 생성한다.

### Spring Data JPA 메소드 이름

- 파생 쿼리 이름이 **30자 이상**이거나, 조건이 **3개 이상**이거나, `OrderBy`가 붙으면 쓰지 않고 `@Query`와 의미 중심 이름으로 바꾼다. (예: `findInPeriod`, `summarizeByCompany`)
- `@Query`는 `XxxJpaRepository`에 `@Override`로 두고, 정렬·범위 포함 여부 같은 의미는 도메인 `XxxRepository`의 메소드 주석에 적는다.
- 파라미터는 `@Param`으로 이름을 명시한다.
- 기준 미만의 짧은 파생 쿼리(`findByUserIdAndStatus`)는 그대로 둔다.

### 계층별 책임

- 서비스는 로직을 호출하는 layer 이다.
- 로직을 수행하는 것은 도메인 layer에서 수행되며, domain 값을 꺼내서 service에서 처리하지 않는다.

## 동시성: 확인한 뒤 처리하지 않는다

- 시도 횟수·쿨다운·중복 방지·일회성처럼 한도를 지키는 로직은 "읽고 판단한 뒤 갱신"으로 쓰지 않는다. 원자 연산(Redis `INCR`/`setIfAbsent`, DB 유니크 제약)을 **먼저** 실행하고 그 결과로 판단한다. 읽는 시점에는 동시 요청이 모두 같은 상태를 보기 때문이다.
- 이런 로직의 테스트는 순차 호출로 끝내지 않는다. "다른 요청이 이미 한도를 소진한 상태"를 Fake에 미리 만들고, 맞는 입력이 와도 거절되는지 확인한다.
- 인증·한도·중복 방지 코드를 새로 쓰거나 바꾼 PR은 만들기 전에 `/code-review`(인증이면 `/security-review`도)를 실행한다. 쓴 사람의 가정과 분리된 눈으로 한 번 더 읽는다.

## OOP 원칙 (코드 작성·수정 시 필수)

코드를 작성하거나 수정하는 모든 요청(새 클래스/메서드, 리팩터링, 설계 제안)에서 `oop-principles` 스킬을 먼저 호출한다. 상세 기준, 사전 스캔 절차, 코드 예시는 스킬에 있다. 오브젝트(조영호) 기반이다.

### 검토 순서

1. **책임과 협력** — 어떤 객체가 이 책임을 가져야 하는가? 정보를 가진 객체(정보 전문가)가 진다.
2. **캡슐화** — getter로 꺼내 외부에서 판단하지 않고 객체에 메시지를 보낸다 (Tell, Don't Ask).
3. **의존성 관리** — 추상화에 의존하고, 내부 `new` 대신 생성자 주입을 쓴다.
4. **합성 vs 상속** — 코드 재사용 목적의 상속은 피하고 합성을 쓴다.
5. **타입 계층** — 상속은 행동 호환(LSP)이 보장될 때만 쓴다. 타입 분기 if/switch는 다형성으로 대체한다 (OCP).

### 코드 생성 규칙

- 원칙에 어긋나는 설계를 발견하면 **코드 생성 전** 사용자에게 알리고 대안을 제시한다.
- 원칙을 타협해야 하면 이유를 명시하고 사용자 승인을 받는다.
- 행동이 없는 단순 CRUD/DTO는 캡슐화 원칙 적용 대상에서 제외한다.
- 추상화(인터페이스·전략)는 **지금 확정된 변형이 둘 이상**일 때만 만든다. "나중에 붙일 수 있다"는 확정이 아니므로 현재 변형 하나만 구현하고, 그 판단을 가정으로 명시한다.

### 기존 코드 수정 시

- 코드를 쓰기 **전에** 수정 대상과 연관 클래스(Service ↔ 엔티티·Repository, Controller ↔ Service·DTO)를 함께 읽고 사전 스캔을 한다. 위반(getter 판단, Service의 도메인 로직)이 있으면 코드 작성 전에 `📋 사전 스캔 결과`로 보고하고 확인을 받는다. 위반이 없으면 보고 없이 진행한다.
- 기존 나쁜 구조(getter 남용, Service의 도메인 로직, String 상태값)를 답습하지 않는다. 리팩터링 후 추가하고 무엇을 왜 바꿨는지 한 줄로 고지한다.
- 타입 분기 if/else에 새 타입 추가 요청이 오면 전략 패턴으로 전환 후 추가하고 고지한다.
- `abstract class` 상속 구조에 `extends` 추가 요청이 오면 합성으로 전환 후 추가하고 고지한다.

### 합리화 함정 — 이 생각이 들면 STOP

| 생각 | 현실 |
|------|------|
| "기존 코드 스타일을 유지해야 한다" | 나쁜 스타일은 유지할 이유가 없다. 리팩터링 후 고지한다. |
| "빠르게 추가하면 되니까 if/else에 넣자" | 빠른 추가가 기술 부채를 만든다. 리팩터링 후 추가한다. |
| "상속 구조가 있으니 extends로 추가하면 된다" | 기존 상속이 잘못됐을 수 있다. 합성으로 전환 후 추가한다. |
| "사용자가 빠르게 해달라고 했다" | 속도와 원칙은 별개다. 올바른 구조로 작성하되 간결하게 한다. |

## 주의사항

- 연관관계가 LAZY라서 컨트롤러/DTO 변환 시점에 `LazyInitializationException`이 발생할 수 있다. 서비스 트랜잭션 안에서 필요한 연관을 초기화할 것.
- CORS 허용 origin은 `CorsConfig`에 하드코딩되어 있다 (localhost:5173, `*.vercel.app`, 프론트엔드 Vercel 도메인). 프론트 도메인이 바뀌면 여기를 수정.
- 테스트는 `@WebMvcTest` + `@MockBean` 서비스 방식의 컨트롤러 테스트와 순수 도메인 단위 테스트로 구성된다. `src/main/resources/application-test.yml`(H2, PostgreSQL 모드)이 있으나 현재 `@SpringBootTest`/`@ActiveProfiles`로 쓰이는 테스트는 없다.

## 테스트 작성

- 테스트 코드를 작성·수정·리뷰할 때는 반드시 `writing-tests` 스킬(`.claude/skills/writing-tests/SKILL.md`)을 먼저 호출해 그 기준을 따른다. 새 로직을 추가하는 기능 작업에서 테스트를 함께 쓸 때도 같다.
- 커버리지 유지선은 라인 70~80%다. 숫자를 맞추려는 테스트를 쓰지 않고, 부족하면 빠진 도메인 규칙·실패 경로 테스트를 찾는다.

## 작업 규칙

- 브랜치·커밋·PR·머지 방식과 형식은 전역 `commit-and-pr-format` 스킬을 따른다. 커밋·PR 작업 전에 스킬을 호출한다.
- 기능 하나가 끝나고 `./gradlew test`가 통과하면 별도 확인 없이 커밋·push·PR 생성까지 하고, 머지는 사용자가 지시하거나 확인한 뒤에 한다. 테스트가 실패하면 커밋/push하지 않는다.
- 커밋 메시지에 `Co-Authored-By` 등 Claude 관련 attribution 줄을 넣지 않는다.
- 작업 중 내용이 바뀔 때마다 그 자리에서 커밋한다. 특히 스킬을 만들거나 고칠 때는 초안, 테스트 후 수정 1회마다 각각 커밋하고, 본문에 "무엇이 잘못 나왔고 왜 바꿨는지"를 적는다. 끝난 뒤 한 번에 커밋하지 않는다. 문제가 자주 생기는 지점을 커밋 이력에서 찾아 개선하기 위해서다.
- 엔티티(`@Entity`/`@Embeddable`)의 테이블·컬럼·관계를 바꾸면 같은 커밋에서 `docs/ERD.md`도 고친다. `.claude/hooks/erd-guard.py`가 엔티티 수정 시 알려주고, ERD 없이 엔티티만 커밋하려 하면 차단한다. `ddl-auto: update`는 컬럼 추가만 하므로 운영 DB에 직접 DDL이 필요한지도 함께 확인한다.
- `fix` 커밋 본문에는 `원인:`, `영향:`, `놓친 이유:`, `재발 방지:`를 항목당 150자 이내로, 신입이 읽어도 이해되게 전문용어를 풀어 적는다(선택: `핵심 파일:`로 변경 전/후를 보여 줄 파일 지정). `.claude/hooks/incident-report.py`가 항목이 없으면 커밋을 차단하고, 있으면 핵심 코드의 변경 전/후가 담긴 `docs/incidents/*.html`을 만들어 같은 커밋에 넣는다. 이미 만든 커밋은 `python .claude/hooks/incident-report.py --from-commit <sha> --set "원인=..."`으로 만든다. 훅 테스트는 `python -m unittest discover -s .claude/hooks -p "test_*.py"`.
