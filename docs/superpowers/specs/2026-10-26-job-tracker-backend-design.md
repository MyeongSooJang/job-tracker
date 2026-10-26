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
| `Company` | 회사 기본 정보 (이름, 업종, 홈페이지) |
| `Application` | 지원 건 관리, 상태 전이 로직 직접 담당 |
| `Schedule` | 면접/과제 일정, 날짜·시간 |
| `Interview` | 면접 회차 정보 |
| `InterviewQuestion` | 질문·답변·메모 |

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

- 모든 엔티티에 `userId` 컬럼 미리 추가 (현재는 단일 사용자, 추후 JWT 기반 계정 시스템 확장)

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
