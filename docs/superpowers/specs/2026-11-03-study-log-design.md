# 학습 기록(StudyLog) 설계 — 백엔드 + 프론트 병렬

## Context
매일 사이트에 들어와 그날 공부한 내용을 정리하며 하루를 마무리하는 기능을 만든다.
아래는 질문으로 확정한 요구사항이다. 추론으로 채운 부분은 "가정"으로 따로 표시했다.

### 확정된 요구사항 (사용자 답변)
| 항목 | 결정 |
|---|---|
| 기록 방식 | 자유 글쓰기 (항목 분리 없음) |
| 에디터 | 마크다운 입력 + 미리보기 |
| 함께 남기는 값 | 공부 시간(분)만. 태그·컨디션·내일 할 일은 제외 |
| 하루 단위 | 하루 1건 (날짜당 글 하나를 이어서 수정) |
| 저장 | 저장 버튼 (자동 저장 없음) |
| 화면 위치 | 사이드바의 독립 메뉴 "학습 기록" (마이페이지 탭 아님) |
| 화면 배치 | 작성 중심: 상단 통계 바, 좌측 오늘 작성 영역, 우측 달력 + 최근 글 |
| 다시 보기 | 달력(날짜별), 최근 글 타임라인, 통계/스트릭 |
| 통계 | 연속 작성일, 이번 달 작성 일수, 주간 공부 시간 차트 |

### 가정 (틀리면 알려 주세요)
1. 공부 시간은 선택 입력이고 비우면 0분으로 저장한다. 글 내용은 필수다.
2. "주간 공부 시간 차트"는 오늘 포함 **최근 7일의 일별 막대 그래프**로 해석한다.
3. 연속 작성일은 오늘 아직 안 썼어도 어제까지 이어졌으면 끊기지 않은 것으로 본다.
4. 미래 날짜는 작성할 수 없고, 과거 날짜 보충 작성은 허용한다. 날짜 기준은 KST다.
5. 최근 글 목록은 최신 10건이고, 더 보기(페이지네이션)는 첫 버전에 넣지 않는다.
6. 검색은 이번 범위에 없다(달력·타임라인만).

## 백엔드 (`job-tracker`)

### 도메인 `domain/studylog`
- `StudyLog` 엔티티: `userId`(`Application`과 같은 방식의 Long), `studyDate`, `content`(TEXT), `studyMinutes`. 유니크 `(user_id, study_date)`.
  `create(user, date, content, minutes, today)` / `update(content, minutes)`. 검증(내용 공백, 시간 0~1440, 미래 날짜)은 엔티티가 맡는다.
- `StudyLogs`(일급 컬렉션): `minutesByDay(from, to)`로 구간별 공부 시간을 빈 날 0분으로 채워 집계한다.
- `StudyStreak`: 기록한 날짜 집합에서 `current(today)`로 연속 작성일을 계산한다(본문을 읽지 않도록 날짜만 조회). 서비스는 getter로 값을 꺼내 계산하지 않고 이 객체들에 메시지만 보낸다.
- `StudyLogRepository`: `findByUserIdAndStudyDate`, `findByUserIdAndStudyDateBetween`, `findTop10ByUserIdOrderByStudyDateDesc`, 삭제.
- `StudyLogJpaRepository`: `JpaRepository` + `StudyLogRepository` 동시 상속 (기존 패턴).
- 오늘 날짜는 `Clock`을 주입해 얻는다(테스트에서 고정 가능).

### 서비스 / 컨트롤러
- `StudyLogService`: 클래스 `readOnly`, 쓰기만 `@Transactional`. 같은 날짜 기록이 있으면 `update`, 없으면 `create`.
- `StudyLogController` `/api/me/study-logs` — 모든 조회는 userId로 한정해 타인 기록 접근 차단.
- DTO: `StudyLogRequest`, `StudyLogResponse`, `StudyStatsResponse` (`service/dto`).
- `ErrorCode`: `STUDY_LOG_NOT_FOUND`(404), `STUDY_CONTENT_REQUIRED`·`INVALID_STUDY_MINUTES`·`FUTURE_STUDY_DATE`(400). 프론트가 한글 메시지를 그대로 보여 주도록 사유별로 분리했다.

### API 계약 (두 저장소의 기준점)
```
StudyLogRequest  { content: string(필수, ≤10000), studyMinutes?: number(0~1440, 기본 0) }
StudyLog         { id, studyDate: "YYYY-MM-DD", content, studyMinutes }   // 갱신 시 flush 전에 응답이 만들어져 audit 시각은 제외
StudyStats       { currentStreak: number, monthWrittenDays: number,
                   last7Days: [{ date: "YYYY-MM-DD", minutes: number }]  // 7개, 오래된 날짜부터 }

PUT    /api/me/study-logs/{date}         -> StudyLog                  작성/수정
GET    /api/me/study-logs/{date}         -> StudyLog | 404 STUDY_LOG_NOT_FOUND
DELETE /api/me/study-logs/{date}         -> 204
GET    /api/me/study-logs?month=YYYY-MM  -> StudyLog[]                달력용, 날짜 오름차순
GET    /api/me/study-logs/recent         -> StudyLog[]                최신 10건, 날짜 내림차순
GET    /api/me/study-logs/stats          -> StudyStats
```
- 오류는 기존 `ErrorResponse({ code, message })`. 날짜는 경로로만 받는다(본문 중복 방지).

### 운영 DB 테이블 (psql로 직접 생성 후 배포 — 사용자 선택)
운영 DB는 실제 서비스 중이고 `.env`(`PG*`)와 `psql`(PostgreSQL 18)로 접근 가능하다. 사용자가 "psql로 직접 생성 후 배포"를 선택했다.
- **순서**: 엔티티 확정 → DDL 작성 → 로컬 검증 → **실행 직전 SQL을 보여 주고 사용자 확인** → 운영 DB에 실행 → 백엔드 PR 머지(배포).
- 엔티티 매핑은 기존 방식(`@Column(columnDefinition = "TEXT")`, 컬럼명은 스네이크 케이스 기본 전략)과 같게 하고, 아래 DDL이 매핑과 어긋나지 않는지 로컬에서 `ddl-auto: update`로 생성한 스키마와 비교한다. 어긋나면 배포 시 Hibernate가 컬럼을 덧붙이거나 오류를 낼 수 있다.
- DDL 초안 (엔티티 확정 후 최종 확인). 다른 테이블(`applications`)처럼 `user_id`는 FK 없이 둔다:
```sql
CREATE TABLE study_logs (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    study_date    DATE         NOT NULL,
    content       TEXT         NOT NULL,
    study_minutes INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(6),
    updated_at    TIMESTAMP(6),
    CONSTRAINT uk_study_logs_user_date UNIQUE (user_id, study_date)
);
```
- 실행 전후로 `\dt`와 기존 테이블 행 수를 기록해 기존 데이터에 영향이 없음을 확인한다. 롤백은 `DROP TABLE study_logs;`(신규 테이블이라 기존 데이터 영향 없음).
- 검증용 데이터는 `e2e-*@example.com` 계정으로만 만들고 끝나면 삭제한다(기존 메모리의 운영 DB 규칙). 운영 계정 `root@job.com` 데이터는 건드리지 않는다.
- 이후 스키마가 바뀌면 같은 절차를 반복한다(마이그레이션 도구가 없음).

## 프론트엔드 (`job-tracker-frontend`)
기존 패턴(axios `apiClient`, TanStack Query 훅, `Sidebar`, shadcn UI, vitest)을 따른다.

| 파일 | 역할 |
|---|---|
| `src/api/types.ts`, `src/api/studyLogs.ts` | 타입과 호출 함수(상세·월별·최근·통계·저장·삭제) |
| `src/hooks/useStudyLogs.ts` | 쿼리 훅 + 저장/삭제 뮤테이션. 성공 시 해당 날짜·월·recent·stats 쿼리 invalidate. 상세 404는 "기록 없음"으로 처리 |
| `src/lib/studyLog.ts` | 순수 함수: 오늘 날짜(KST), 월 문자열, 분 → "N시간 M분" |
| `src/pages/StudyLogs.tsx` | 페이지. 선택 날짜 state(기본 오늘)와 월 state 보유 |
| `src/components/studylog/StudyStatsBar.tsx` | 상단: 연속 작성일, 이번 달 작성 일수, 주간 차트 |
| `src/components/studylog/WeeklyMinutesChart.tsx` | 최근 7일 막대. 라이브러리 없이 SVG/CSS (`dataviz` 스킬 기준 참고) |
| `src/components/studylog/StudyLogEditor.tsx` | 날짜 헤더, 공부 시간 입력, 마크다운 입력/미리보기 전환, 저장·삭제 버튼 |
| `src/components/studylog/StudyLogCalendar.tsx` | 월 달력, 기록 있는 날 표시, 날짜 클릭 시 선택. `schedules/InterviewCalendar.tsx` 구조 확인 후 재사용 가능한 부분만 활용 |
| `src/components/studylog/RecentStudyLogs.tsx` | 최근 글 목록(날짜 + 첫 줄 요약), 클릭 시 해당 날짜 선택 |
| `src/components/layout/Sidebar.tsx`, `src/App.tsx` | 메뉴 "학습 기록"과 라우트 `/study-logs` 추가 (`RequireAuth` 안쪽) |
| 마크다운 렌더 | `react-markdown` 1개 추가 (미리보기와 저장된 글 보기에 사용, 원본 HTML은 렌더하지 않음) |

테스트(vitest + testing-library): `lib/studyLog`, `api/studyLogs` 호출 경로, `StudyLogEditor`(저장·빈 내용 막기·미리보기 전환), `Sidebar` 메뉴 노출.

## 병렬 진행 방식
1. 위 API 계약을 먼저 고정한다. 계약이 바뀌면 두 저장소에 모두 반영한다.
2. 두 저장소 모두 `main`에서 `feat/study-log` 브랜치를 따고 각자 PR을 만든다.
3. 프론트는 `api/studyLogs`를 모킹해 백엔드 없이 개발·테스트한다.
4. 통합 확인은 백엔드 `bootRun` + 프론트 `.env.local`의 API 주소를 로컬로 맞춰 수행한다.
5. 머지 순서는 백엔드 PR → 배포 → 프론트 PR (프론트가 먼저 나가면 새 메뉴가 404를 맞는다).
6. 구현은 각 저장소에서 `oop-principles`·`writing-tests` 스킬(백엔드), TDD 순서(도메인 → 서비스 → 컨트롤러 / 순수 함수 → API → 컴포넌트)로 진행하고, `commit-and-pr-format`을 따른다.

## 사용할 스킬·도구 (단계별)
| 단계 | 스킬/도구 | 용도 |
|---|---|---|
| 설계 마무리 | `superpowers:brainstorming` (진행 중) | 승인 후 이 계획을 `docs/superpowers/specs/2026-11-03-study-log-design.md`로 저장·커밋 |
| 구현 계획 | `superpowers:writing-plans` | 백엔드·프론트 각각의 작업 단위 계획서 작성 (기존 `docs/superpowers/plans/` 방식) |
| 백엔드 구현 | `oop-principles` | 프로젝트 CLAUDE.md가 코드 작성 전 호출을 필수로 정함. `StudyLogs` 일급 컬렉션·Tell Don't Ask 점검 |
| 테스트 | `superpowers:test-driven-development` + `writing-tests` | 실패 테스트 먼저 작성. 한글 메서드명, 계층별 테스트 기준 |
| 프론트 UI | `frontend-design:frontend-design` | 기존 디자인 토큰(brand/ink/line)과 shadcn 컴포넌트에 맞춰 화면 구성. 새 스타일을 만들지 않음 |
| 차트 | `dataviz` | 7일 막대 차트의 색·축·접근성 기준 |
| 운영 DB | Bash의 `psql` (`.env` 접속 정보) | DDL 실행 전 SQL 확인을 받음. 별도 플러그인·MCP는 쓰지 않음 |
| 통합 확인 | `run` + `claude-in-chrome` 또는 `anthropic-skills:built-in-browser` | 로컬 백엔드·프론트를 띄워 브라우저로 작성·저장·달력·통계 흐름 확인 |
| 완료 전 | `superpowers:verification-before-completion` | 테스트·빌드 명령을 실제로 돌려 통과를 확인한 뒤에만 완료 보고 |
| 리뷰 | `code-review` (low~medium) | PR 전에 변경분 점검 |
| 커밋·PR | `commit-and-pr-format` | 브랜치·커밋·PR 형식(Co-Authored-By·서명 줄 제외 규칙 포함), 두 저장소에 각각 PR |
| 마무리 | `superpowers:finishing-a-development-branch` | 머지·정리 선택지 제시 |

- 병렬 작업: 백엔드와 프론트를 서브에이전트로 동시에 돌릴지는 **사용자가 정한다**(`superpowers:dispatching-parallel-agents`). 기본은 한 세션에서 API 계약 기준으로 번갈아 진행하고, 요청하시면 에이전트로 나눈다.
- 쓰지 않는 것: Notion/Canva/Gmail 등 외부 MCP, `using-git-worktrees`(저장소가 이미 둘로 분리돼 있어 불필요).

## 테스트
- 백엔드: `StudyLogTest`(공백·시간 범위·미래 날짜·update), `StudyLogsTest`(스트릭: 오늘 미작성+어제까지 연속, 하루 공백으로 끊김, 월 작성 일수, 7일 구간 0 채우기), `StudyLogControllerTest`(PUT 성공/400, GET 404, 월별, recent, stats, DELETE), 서비스 upsert 분기.
- 프론트: 위 vitest 목록.

## 검증
- 백엔드 `./gradlew test`, `./gradlew jacocoTestCoverageVerification`(70%) 통과, Swagger UI에서 PUT → GET → 월별 → recent → stats → DELETE 확인.
- 프론트 `npm test`, `npm run build` 통과. 로컬 백엔드와 연결해 사이드바 → 학습 기록에서 작성·수정·삭제, 달력 날짜 이동, 마크다운 미리보기, 통계 갱신을 브라우저로 확인.
