# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

취업 활동(서류 제출 ~ 면접) 기록 관리용 Spring Boot 3.3 / Java 21 백엔드 (Gradle, JPA, PostgreSQL). 프론트엔드는 별도 저장소(Vercel 배포)이며, 설계 문서는 `docs/superpowers/specs/`, 구현 계획은 `docs/superpowers/plans/`에 있다.

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
- 테스트 메서드명은 한글(`POST_applications_지원_건을_등록한다`) 스타일을 따른다.

## 아키텍처

패키지는 `com.jobtracker` 아래 계층형 + DDD 풍으로 나뉜다.

- `presentation/controller` — REST 컨트롤러 (`/api/**`)
- `application/service` — 트랜잭션 서비스 (클래스 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`), 요청 DTO는 `service/dto`
- `domain/<aggregate>` — 엔티티, enum, **Repository 인터페이스**. 비즈니스 규칙은 엔티티/enum이 담당 (예: `ApplicationStatus.canAdvanceTo`로 상태 전이 검증)
- `infrastructure/persistence` — `XxxJpaRepository`가 `JpaRepository`와 도메인 `XxxRepository` 인터페이스를 **함께 상속**하는 구조 (별도 어댑터 구현 없음)
- `global` — `CorsConfig`, `JpaConfig`(`@EnableJpaAuditing`), `BusinessException` + `ErrorCode` + `GlobalExceptionHandler`

도메인 관계: `Company` → `Application` → (`Schedule`, `Interview` → `InterviewQuestion`). 엔티티는 정적 팩토리 `create(...)`로 생성한다.

### 계층별 책임

- 서비스는 로직을 호출하는 layer 이다.
- 로직을 수행하는 것은 도메인 layer에서 수행되며, domain 값을 꺼내서 service에서 처리하지 않는다.

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
