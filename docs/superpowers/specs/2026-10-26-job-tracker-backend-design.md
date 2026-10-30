# Job Tracker 백엔드 설계 문서

**작성일:** 2026-10-26  
**작성자:** 장명수  
**범위:** Spring Boot 백엔드 (프론트엔드 별도 설계)

---

## 1. 프로젝트 개요

취업 활동 중 서류 제출 ~ 면접까지의 과정을 체계적으로 기록·관리하는 웹 서비스의 백엔드.  
현재 노션 대비 가시성과 작성 편의성을 높이는 것이 목표.  
초기에는 단일 사용자, 이후 다중 계정으로 확장 가능한 구조로 설계.

---

## 2. 기술 스택

| 영역 | 선택 |
|---|---|
| Framework | Spring Boot 3.x |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate |
| DB | PostgreSQL |
| 빌드 도구 | Gradle |
| API 문서화 | Springdoc OpenAPI (Swagger UI) |
| 배포 (초기) | Railway (무료) |
| 배포 (확장) | AWS EC2 |

---

## 3. 도메인 모델

### 도메인 구조

```
Company (회사)
  └─ Application (지원 건)
       ├─ Schedule (일정)
       └─ Interview (면접)
            └─ InterviewQuestion (질문/답변)
```

### 각 도메인 책임

| 도메인 | 책임 |
|---|---|
| `Company` | 회사 기본 정보 (이름, 업종, 홈페이지). 사용자 공용 데이터 |
| `Application` | 지원 건 관리, 상태 전이 로직 직접 담당 |
| `Schedule` | 면접/과제 일정, 날짜·시간 |
| `Interview` | 면접 회차 정보 |
| `InterviewQuestion` | 질문·답변·메모 |
| `Review` | 지원 건당 1개의 후기와 평점 (1~5) |

### Application 상태 전이

```
RESUME_SUBMITTED (서류접수)
  → RESUME_PASSED (서류합격)
  → INTERVIEW_1 (1차 면접)
  → INTERVIEW_2 (2차 면접)
  → FINAL (최종)
  → ACCEPTED (최종합격) / REJECTED (불합격)
```

- 상태 전이 규칙(`canAdvance()`, `advance()`)은 `Application` 엔티티 내부에서 처리
- Service에서 getter로 꺼내 판단하는 Tell, Don't Ask 위반 금지

### DB 관계

```
Company    1 ─── N  Application
Application 1 ─── N  Schedule
Application 1 ─── N  Interview
Interview   1 ─── N  InterviewQuestion
```

### 확장 준비

- 사용자별 데이터(`Application` 등)에는 `userId` 컬럼을 미리 둔다 (현재는 단일 사용자, 추후 JWT 기반 계정 시스템 확장)
- `Company`는 공용 데이터이므로 `userId`를 두지 않는다 (8장 참고)

---

## 4. 레이어 구조

```
presentation/   ← REST Controller, 요청/응답 DTO
application/    ← UseCase/Service (흐름 조율)
domain/         ← Entity, Repository 인터페이스, 도메인 로직
infrastructure/ ← JPA Repository 구현체, DB 설정
```

---

## 5. 패키지 구조

```
src/main/java/com/yourname/jobtracker/
├── presentation/
│   └── controller/
│       ├── CompanyController
│       ├── ApplicationController
│       ├── InterviewController
│       └── ScheduleController
├── application/
│   └── service/
│       ├── CompanyService
│       ├── ApplicationService
│       ├── InterviewService
│       └── ScheduleService
├── domain/
│   ├── company/
│   │   ├── Company
│   │   └── CompanyRepository
│   ├── application/
│   │   ├── Application
│   │   ├── ApplicationStatus
│   │   └── ApplicationRepository
│   ├── interview/
│   │   ├── Interview
│   │   ├── InterviewQuestion
│   │   └── InterviewRepository
│   └── schedule/
│       ├── Schedule
│       └── ScheduleRepository
└── infrastructure/
    └── persistence/
        ├── CompanyJpaRepository
        ├── ApplicationJpaRepository
        ├── InterviewJpaRepository
        └── ScheduleJpaRepository
```

---

## 6. 핵심 API 목록

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/companies` | 회사 등록 |
| `GET` | `/api/companies` | 회사 목록 조회 |
| `GET` | `/api/companies/{id}` | 회사 단건 조회 |
| `POST` | `/api/applications` | 지원 건 등록 |
| `GET` | `/api/applications` | 지원 현황 목록 |
| `GET` | `/api/applications/{id}` | 지원 건 단건 조회 |
| `PATCH` | `/api/applications/{id}/status` | 상태 전이 |
| `POST` | `/api/applications/{id}/interviews` | 면접 등록 |
| `GET` | `/api/applications/{id}/interviews` | 면접 목록 조회 |
| `POST` | `/api/applications/{id}/interviews/{interviewId}/questions` | 질문/답변 추가 |
| `GET` | `/api/applications/{id}/interviews/{interviewId}/questions` | 질문/답변 목록 |
| `POST` | `/api/schedules` | 일정 등록 |
| `GET` | `/api/schedules` | 일정 목록 (날짜 범위 필터) |

---

## 7. 확장 계획 (Phase 2)

- JWT 기반 회원 인증/인가 추가
- 다중 사용자 지원
- AWS EC2 배포 전환

---

## 8. 회사 평점·후기 및 공유 준비 (2026-10-30 추가)

다른 사용자와의 공유를 대비한 구조 준비 단계. 로그인과 타인 후기 열람은 이번 범위에 넣지 않는다.

- **Company 공용화:** `userId` 제거, `name` 유니크. 같은 이름 등록 시 `COMPANY_ALREADY_EXISTS`.
- **Review:** `rating`(1~5 정수, 후기 작성 전 null) 추가. 후기는 지원 건마다 1개(기존 1:1 유지)이고, 한 회사에 여러 번 지원하면 지원 건마다 후기를 쓴다.
- **공개 정책:** 비공개 옵션은 없다. 작성된 후기는 공개 대상이며, `visibility` 필드와 enum은 만들지 않는다.
- **도메인 규칙:** `review.write(content, rating)`가 평점 범위(1~5)를 검증하고 위반 시 `INVALID_RATING`을 던진다.
- **API:** `PATCH /api/reviews/{id}`가 `content`와 `rating`을 모두 필수로 받는다 (`rating`은 `@Min(1) @Max(5)`).
- **범위 밖:** 회사별 평균 평점, 타인 후기 열람, 로그인(JWT), 익명 처리는 Phase 2에서 다룬다.
- **배포 주의:** `ddl-auto: update`라서 기존 DB에 같은 이름의 회사가 있으면 유니크 제약 추가가 실패한다. 배포 전 중복 여부를 확인한다.
