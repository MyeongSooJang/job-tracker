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

## 주의사항

- 연관관계가 LAZY라서 컨트롤러/DTO 변환 시점에 `LazyInitializationException`이 발생할 수 있다 (최근 수정 이력: `ce2bdd4`). 서비스 트랜잭션 안에서 필요한 연관을 초기화할 것.
- CORS 허용 origin은 `CorsConfig`에 하드코딩되어 있다 (localhost:5173, `*.vercel.app`, 프론트엔드 Vercel 도메인). 프론트 도메인이 바뀌면 여기를 수정.
- 테스트는 `@WebMvcTest` + `@MockBean` 서비스 방식의 컨트롤러 테스트와 순수 도메인 단위 테스트로 구성된다. `src/main/resources/application-test.yml`(H2, PostgreSQL 모드)이 있으나 현재 `@SpringBootTest`/`@ActiveProfiles`로 쓰이는 테스트는 없다.

## 작업 규칙

- 기능 하나가 끝나고 `./gradlew test`가 통과하면 별도 확인 없이 커밋하고 현재 브랜치로 push한다. 커밋 메시지는 기존 이력처럼 `feat:`/`fix:` 접두사 + 한글로 작성한다. 테스트가 실패하면 커밋/push하지 않는다.
- 커밋 메시지에 `Co-Authored-By` 등 Claude 관련 attribution 줄을 넣지 않는다.
