---
name: writing-tests
description: Use when writing, modifying, or reviewing test code in this project ("테스트 작성해줘", "테스트 추가", "커버리지 올려줘", "테스트 리뷰"), or when adding logic that needs tests. Defines 테스트 품질 기준, 계층별 테스트 방식, 작성 템플릿, 완료 체크리스트.
---

# Writing Tests

커버리지 숫자가 아니라 **"버그를 실제로 잡아내는가"**가 기준이다. 이 프로젝트의 커버리지 유지선은 **70~80%**이며, 이는 좋은 테스트의 결과로 따라오는 값이지 목표가 아니다. (아래 "커버리지에 대한 태도" 참고)

근거: Martin Fowler (TestCoverage, Practical Test Pyramid, UnitTest, Mocks Aren't Stubs), Google (Software Engineering at Google 12·13장, Code Coverage Best Practices), Kent Beck (Test Desiderata), PIT Mutation Testing, Spring Boot 공식 테스트 문서.

## 핵심 원칙

1. **관찰 가능한 행동을 검증한다.** public 메서드의 결과(반환값, 상태 변화, 예외/에러 코드)를 검증한다. 내부 구현(호출 순서, private 필드)에 결합하지 않는다. 리팩터링할 때마다 깨지는 테스트는 안전망이 아니다.
2. **단언 없는 테스트를 쓰지 않는다.** "예외 없이 실행된다"는 테스트가 아니다. 실행만 하고 검증이 없으면 커버리지만 오르고 버그는 못 잡는다.
3. **테스트 하나에 조건 하나.** Given-When-Then 구조로 쓴다.
4. **가장 낮은 계층에서 검증한다.** 규칙은 도메인 단위 테스트에서 끝낸다. 같은 규칙을 컨트롤러/통합 테스트에서 반복하지 않는다.
5. **사소한 코드는 테스트하지 않는다.** 단순 getter, DTO, 설정 클래스를 커버리지 때문에 테스트하지 않는다.
6. **private 메서드를 직접 테스트하지 않는다.** 그러고 싶다면 클래스 책임이 많다는 신호이므로 클래스를 분리한다.
7. **가독성이 DRY보다 우선이다.** 테스트 안에서 무엇을 검증하는지 읽혀야 한다. 같은 준비 코드는 3번 반복된 뒤에 헬퍼로 뽑는다.
8. **중복되거나 구현에 결합된 테스트는 지운다.** 테스트 하나하나가 유지보수 비용이다.
9. **상태를 검증하고, 상호작용(`verify`)은 최소화한다.** 호출 여부 자체가 요구사항인 경우(예: 중복이면 `save`를 호출하지 않는다)에만 쓴다. 상태를 바꾸는 호출에 한정하고, 조회 호출은 verify하지 않는다.
10. **테스트 안에 로직(반복문, 조건문)을 넣지 않는다.** 로직이 있으면 테스트 자체가 틀릴 수 있다. 여러 입력은 `@ParameterizedTest`로 처리한다.
11. **결정적으로 만든다.** 현재 시각·난수를 코드 안에서 직접 읽지 않고 값을 주입한다. 테스트끼리 서로 의존하거나 실행 순서에 의존하지 않는다.

## 계층별 방식

| 대상 | 방식 | 비중 |
|---|---|---|
| `domain/*` 엔티티·enum (비즈니스 규칙) | Spring 없는 순수 단위 테스트. 실제 협력 객체 사용 | **가장 많이** |
| `presentation/controller` | `@WebMvcTest` + `@MockBean` 서비스. 요청 매핑, 검증, 상태코드, 에러 응답 | 적당히 |
| `application/service` | `@ExtendWith(MockitoExtension.class)`. 도메인 호출과 협력 흐름 위주 (로직은 도메인에 있으므로 얇게) | 적게 |
| `infrastructure/persistence` | 쿼리 메서드가 직접 정의된 경우에만 | 필요한 만큼 |
| 전체 연동 `@SpringBootTest` | 핵심 흐름 소수만. 컨텍스트가 새로 뜨므로 느리다 | 아주 적게 |

- 이 프로젝트는 Spring Boot 3.3.x라서 `@MockBean`을 쓴다. `@MockitoBean`은 3.4부터이므로 Boot를 올릴 때 전환한다.
- 서비스 테스트에서 서비스가 값을 꺼내 판단하는 로직이 필요해 보이면 테스트가 아니라 **서비스 설계 문제**다. 도메인으로 옮긴다. (CLAUDE.md "계층별 책임")
- 테스트 대역의 우선순위는 **실제 객체 → Fake → Mock** 순이다. 실제 객체가 빠르고 결정적이면 그것을 쓴다. 도메인 객체끼리는 mock으로 대체하지 않는다. mock 대상은 DB, 네트워크, 시간처럼 느리거나 비결정적인 것이다. 이미 `FakePasswordHasher`처럼 Fake가 있으면 mock보다 재사용한다.
- mock을 쓸 때도 `given`으로 정한 값을 그대로 되돌려 확인하지 않는다. 그런 테스트는 구현을 복제하는 거울일 뿐이다. 결과 상태·예외·소유권 같은 **관찰 가능한 결과**를 검증한다.

## 도메인에서 반드시 확인할 케이스

- 정상 경로 1개
- **경계값** (0, 빈 문자열, 최대 길이, 날짜 경계)
- **실패 경로**: 던지는 예외의 `ErrorCode`까지 검증한다 (예외 타입만 보지 않는다)
- 상태 전이: 허용되는 전이와 **허용되지 않는 전이 모두**. 조합이 많으면 `@ParameterizedTest`
- 잘못된 입력/null 처리

## 네이밍

**행동 중심**으로 짓는다. 메서드 이름이 아니라 "어떤 상황에서 무엇이 보장되는가"를 한글 문장으로 쓴다. 메서드 하나에 행동이 여러 개일 수 있으므로 메서드명 접두사는 붙이지 않는다. 이름에 "그리고"가 필요하면 테스트를 쪼갠다.

```
지원을_새로_만들면_서류_제출_상태이다
면접_중인_지원은_합격으로_전이할_수_있다
이미_합격한_지원은_다시_면접으로_전이할_수_없다
다른_사용자의_지원_건은_조회할_수_없다
```

- 컨트롤러 테스트는 HTTP 요청과 응답을 행동으로 기술한다: `유효한_요청이면_201과_등록된_회사를_반환한다`, `토큰이_없으면_401을_반환한다`
- 기존 테스트의 `메서드_행동` 이름은 해당 테스트를 수정할 때 행동 중심으로 바꾼다. 이름만 바꾸려고 별도 작업을 만들지 않는다.

## 템플릿

### 1. 도메인 단위 테스트

```java
class ApplicationTest {

    private Application 면접_상태_지원() {
        Application app = Application.create(1L, 회사(), "백엔드 개발자", LocalDate.now());
        app.advanceStatus(ApplicationStatus.INTERVIEW);
        return app;
    }

    @Test
    void 면접_중인_지원은_합격으로_전이할_수_있다() {
        // given
        Application app = 면접_상태_지원();

        // when
        app.advanceStatus(ApplicationStatus.ACCEPTED);

        // then
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 이미_합격한_지원은_다시_면접으로_전이할_수_없다() {
        // given
        Application app = 면접_상태_지원();
        app.advanceStatus(ApplicationStatus.ACCEPTED);

        // when & then
        assertThatThrownBy(() -> app.advanceStatus(ApplicationStatus.INTERVIEW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @ParameterizedTest
    @MethodSource("허용되지_않는_전이")
    void 종료된_지원은_다른_상태로_전이할_수_없다(ApplicationStatus from, ApplicationStatus to) {
        // 입력 조합이 많을 때 @ParameterizedTest 로 한 규칙을 한 곳에서 검증한다
    }
}
```

### 2. 컨트롤러 테스트 (`@WebMvcTest`)

```java
@WebMvcTest(CompanyController.class)
@Import(SecurityConfig.class)
class CompanyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @MockBean CompanyService companyService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void 유효한_요청이면_201과_등록된_회사를_반환한다() throws Exception {
        // given
        given(companyService.register(any())).willReturn(Company.create("카카오", "IT", "https://kakao.com"));

        // when & then: 상태코드와 응답 본문 핵심 필드를 함께 검증한다
        mockMvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "카카오", "industry": "IT", "website": "https://kakao.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("카카오"));
    }

    @Test
    void 이름이_비어있으면_400을_반환한다() throws Exception {
        // 입력 검증 실패 경로
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        // 인증 실패 경로
    }
}
```

컨트롤러 테스트는 **HTTP 계약**(매핑, 검증, 상태코드, 에러 포맷, 인증)만 검증한다. 비즈니스 규칙은 여기서 다시 검증하지 않는다.

### 3. 서비스 테스트 (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @InjectMocks ApplicationService applicationService;

    @Test
    void 소유자의_지원은_요청한_상태로_전이된다() {
        // given
        Application app = Application.create(1L, 회사(), "백엔드 개발자", LocalDate.now());
        given(applicationRepository.findById(10L)).willReturn(Optional.of(app));

        // when
        applicationService.advanceStatus(1L, 10L, new ApplicationStatusRequest(ApplicationStatus.ACCEPTED));

        // then: 서비스가 도메인에 위임했는지를 결과 상태로 확인한다
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 존재하지_않는_지원은_조회할_수_없다() {
        given(applicationRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.findById(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }
}
```

## 안티패턴 (보이면 고친다)

| 안티패턴 | 이유 | 대신 |
|---|---|---|
| 단언 없음 / `assertThat(x).isNotNull()`만 | 실행만 하고 검증하지 않음 | 값과 상태를 구체적으로 검증 |
| `verify(...)`만으로 끝나는 서비스 테스트 | 구현 결합, 결과를 모름 | 결과 상태 검증 우선, verify는 부수효과(저장, 알림) 확인용 |
| 한 테스트에 `when`이 여러 번 | 실패 원인이 모호함 | 조건별로 분리 |
| 도메인 규칙을 컨트롤러 테스트에서 검증 | 계층 중복 | 도메인 테스트로 이동 |
| 예외 타입만 확인 | 다른 사유의 예외도 통과 | `ErrorCode`까지 확인 |
| 커버리지를 위한 getter/DTO 테스트 | 가치 없는 유지 비용 | 작성하지 않음 |
| 현재 시각/랜덤에 의존 | 비결정적 | 고정된 값을 주입 |
| 테스트 안의 반복문·조건문 | 테스트가 틀릴 수 있음 | 단순한 직선 코드, 여러 입력은 `@ParameterizedTest` |
| `given`으로 정한 값을 그대로 되돌려 확인 | 구현을 복제하는 거울 테스트 | 결과 상태·예외·소유권 같은 관찰 가능한 결과 검증 |
| 테스트 이름이 메서드명 나열 | 어떤 행동을 보장하는지 알 수 없음 | 상황과 보장되는 행동을 문장으로 |
| 테스트가 다른 테스트의 실행 순서에 의존 | 깨지기 쉬움 | 각 테스트가 스스로 준비 |

## 완료 체크리스트

테스트를 작성/수정했다면 끝내기 전에 확인한다.

- [ ] 모든 테스트에 구체적인 단언이 있다
- [ ] 정상 경로뿐 아니라 경계값과 실패 경로가 있다
- [ ] 실패 경로는 예외 타입이 아니라 `ErrorCode`까지 검증한다
- [ ] 검증을 가장 낮은 계층에서 한다 (중복 없음)
- [ ] 내부 구현이 아니라 관찰 가능한 행동에 결합되어 있다
- [ ] 테스트 이름이 행동을 문장으로 설명한다 (메서드명 나열이 아님)
- [ ] **판별 질문**: 구현의 핵심 줄(예: `verifyOwner(userId)` 호출, 분기 조건)을 지우거나 비틀면 이 테스트가 실패하는가? 실패하지 않으면 보강한다
- [ ] 테스트 안에 반복문·조건문이 없고, 시각·난수에 의존하지 않는다
- [ ] `./gradlew test`가 통과한다
- [ ] 새 로직이 있다면 해당 로직의 분기가 모두 테스트에 의해 실행된다

## 커버리지에 대한 태도

- **유지선은 70~80%**다. Google은 60% 수용 가능, 75% 권장, 90% 모범 수준으로 보고 팀 상황에 맞게 정하라고 하며, 우아한형제들은 커버리지를 품질의 긍정 지표가 아니라 낮을 때 문제를 알리는 부정 지표로 쓴다. 70~80%는 이 흐름에 맞는 현실적인 선이다.
- 70% 아래로 떨어지면 신호다. 숫자를 올리려 하지 말고 **빠진 로직 테스트**(도메인 규칙, 실패 경로)가 무엇인지 먼저 찾는다.
- 커버리지 리포트는 **테스트되지 않은 코드를 찾는 도구**로 쓴다. 비어 있는 곳이 위험한 로직인지 판단하고, 위험하면 테스트를 추가한다.
- 숫자를 맞추려고 의미 없는 테스트를 쓰지 않는다. getter, DTO, 설정 클래스는 JaCoCo에서 제외하는 것이 낫다.
- 80%를 넘겨 올리는 것에 힘쓰지 않는다. 그 구간은 수익이 줄고 가치 없는 테스트가 늘기 쉽다.
- 커버리지가 높아도 단언이 약하면 가치가 없다. 의심되면 뮤테이션 테스트(PIT)로 확인한다.
