---
name: oop-principles
description: Use when writing, modifying, or refactoring Java/Spring code ("클래스 작성", "메서드 추가", "리팩터링", "기능 추가", "설계 제안"), or when a Service touches entity getters, branches on type with if/switch, or reuses code via extends. Defines 사전 스캔 절차, 레거시 수정 규칙, 오브젝트(조영호) 기반 5대 원칙 상세와 코드 예시.
---

# OOP Principles — 오브젝트(조영호) 기반

핵심 절차(5단계 검토 순서, 코드 생성 규칙, 합리화 함정)는 프로젝트 `CLAUDE.md`에 있다. 이 문서는 코드를 쓰거나 고치기 전에 펼쳐보는 상세 기준이다.

## 수정 전 사전 스캔 절차 (코드 작성 전 필수)

기존 코드에 기능을 추가하거나 수정하는 요청이 오면, **코드를 작성하기 전에** 아래 절차를 반드시 수행한다.

### Step 1: 요청 파일 + 연관 클래스 함께 읽기

수정 대상 파일만 읽지 않는다. 아래 규칙으로 연관 클래스를 함께 읽는다.

| 수정 대상 | 함께 읽어야 할 클래스 |
|-----------|----------------------|
| `XxxService` | `Xxx` 엔티티, `XxxRepository`, Service가 사용하는 다른 도메인 객체 |
| `XxxController` | `XxxService`, 관련 Request/Response DTO |
| `Xxx` 엔티티 | 이 엔티티를 사용하는 Service 클래스 |

### Step 2: 연관 클래스 스캔 — 이 패턴들을 찾는다

**Tell, Don't Ask 위반 스캔** — Service에서 다음 패턴이 있으면 위반:
```java
// ❌ 위반 패턴: getter로 꺼내서 Service가 판단
if (xxx.getStatus() == SomeStatus.VALUE) { ... }
if (xxx.getType().equals("VALUE")) { ... }
if (xxx.getAmount() > 0 && xxx.getStatus() != ...) { ... }
return xxx.getField() != null && !xxx.getField().isBlank();
```

**도메인 로직 Service 집중 스캔** — Service에 있으면 엔티티로 이동 대상:
- 금액 계산, 할인 계산, 수수료 계산
- 상태 전이 가능 여부 판단 (`canXxx()`, `isXxx()`)
- 유효성 검증 (`validateXxx()`)
- 비즈니스 규칙 ("주문은 결제 완료 후에만 취소 가능" 등)

**엔티티 누락 메서드 스캔** — Service가 getter로 꺼내서 하는 일을 엔티티가 직접 할 수 있는가:
- `entity.getStatus() == X` → `entity.isX()` 메서드가 있는가?
- `entity.getField() != null` → `entity.hasField()` 메서드가 있는가?
- `entity.get*()` 여러 개 조합 → `entity.validateXxx()` 메서드가 있는가?

### Step 3: 스캔 결과 보고 후 코드 작성

스캔에서 위반 패턴이 발견되면, **코드를 작성하기 전에** 다음 형식으로 보고한다:

```
📋 사전 스캔 결과
- [파일명:라인] 위반 패턴 설명 → 권장 수정 방향
- [파일명:라인] 위반 패턴 설명 → 권장 수정 방향

요청하신 기능 추가와 함께 위 항목도 수정할까요? (아니면 요청 부분만 수정할게요.)
```

위반이 없으면 보고 없이 바로 코드를 작성한다.

---

## 레거시 코드 수정 요청 시 규칙 (핵심)

기존 코드에 기능을 추가하거나 수정하는 요청에서 아래 3가지 위반 패턴을 **코드 작성 전에** 반드시 확인한다.

### 규칙 1: 기존 나쁜 구조를 그대로 답습하지 않는다

기존 코드에 getter/setter 남용, Service에 도메인 로직 집중, String 타입 상태값 등이 있어도 **그 스타일로 기능을 추가하지 않는다.**

원칙에 맞게 리팩터링 후 기능을 추가하고, 무엇을 왜 바꿨는지 한 줄로 고지한다.

❌ 기존 나쁜 패턴 답습
```java
public int calculateDiscount(Long orderId) {
    if ("VIP".equals(order.getMemberGrade())) return ... // getter로 꺼내 외부 판단
}
```

✅ 원칙에 맞게 수정 후 고지
```java
public class Order {
    public int calculateDiscount() { return grade.applyDiscount(price * quantity); }
}
// → "도메인 로직을 Order 객체로 이동했습니다. 원래 방식이 필요하면 말씀해주세요."
```

### 규칙 2: "빠르게" 요청에도 OCP 위반 코드로 작성하지 않는다

if/else나 switch로 타입을 분기하는 기존 코드에 새 타입 추가 요청이 오면, **전략 패턴으로 리팩터링 후 추가하고 고지한다.**

❌ 빠르다는 이유로 기존 if/else에 추가
```java
} else if (paymentType.equals("KAKAO_PAY")) { ... }
```

✅ 전략 패턴으로 전환 후 추가하고 고지
```java
public interface PaymentProcessor {
    boolean supports(String type);
    PaymentResult pay(int amount);
}
// → "OCP 위반 구조여서 전략 패턴으로 전환 후 추가했습니다. 기존 방식이 필요하면 말씀해주세요."
```

### 규칙 3: 상속 구조에 extends 추가 요청이 오면 합성으로 전환 후 추가한다

`abstract class` 상속으로 공통 로직을 재사용하는 기존 구조에 새 클래스 추가 요청이 오면, **합성으로 전환 후 기능을 추가하고 고지한다.**

❌ 기존 상속 구조 무비판적 답습
```java
public class PushNotificationService extends BaseNotificationService { ... }
```

✅ 합성으로 전환 후 추가하고 고지
```java
public class NotificationService {
    private final Map<NotificationChannel, NotificationSender> senders;
    private final RetryTemplate retryTemplate;
    private final NotificationRepository repository;
}
// → "코드 재사용 목적의 상속을 합성으로 전환했습니다. 기존 구조가 필요하면 말씀해주세요."
```


## 각 원칙 상세

### 1. 역할·책임·협력 (3·5장)

**핵심 원칙**
- 객체를 설계할 때 **데이터(상태)가 아닌 행동(책임)**에서 시작한다.
- 메시지를 먼저 결정하고, 그 메시지를 처리할 객체를 선택한다. (메시지가 객체를 선택한다)
- 책임은 **정보 전문가(INFORMATION EXPERT)** 에게 할당한다: 책임 수행에 필요한 정보를 가진 객체가 그 책임을 진다.
- 정보를 알고 있다고 해서 반드시 그 정보를 저장할 필요는 없다. 처리 불가한 작업은 외부에 위임한다.
- 하나의 객체는 하나의 이유로만 변경된다 (SRP).
- 객체는 스스로 책임을 수행하는 **자율적인** 존재다 — 외부에서 데이터를 꺼내 처리하지 않는다.

**Spring Boot 안티패턴**

❌ Service가 도메인 로직을 직접 구현 (빈약한 도메인 모델)
```java
public class OrderService {
    public int calculateDiscount(Order order) {
        if (order.getMemberGrade().equals("VIP")) return order.getPrice() * 10 / 100;
        return 0;
    }
}
```

✅ 도메인 객체가 자신의 책임을 수행
```java
public class Order {
    public int calculateDiscount() {
        return member.applyDiscount(this.price);
    }
}
```

**체크리스트**
- [ ] 이 메서드/클래스는 하나의 이유로만 변경되는가?
- [ ] 책임이 정보를 가진 객체에 할당되었는가?
- [ ] Service 레이어가 도메인 로직을 직접 구현하지 않는가?
- [ ] 객체가 스스로 처리하는가? (데이터를 꺼내달라는 요청이 아닌)

---

### 2. 캡슐화 (4·14장)

**핵심 원칙**
- 캡슐화는 단순히 `private` 필드 + getter/setter가 아니다. **변할 수 있는 모든 것을 감추는 것**이다.
- 속성을 `private`으로 선언했더라도 getter로 꺼내서 외부에서 조작하면 캡슐화가 깨진 것이다.
- 객체의 퍼블릭 인터페이스와 구현을 분리한다: 인터페이스만 알아도 사용 가능해야 한다.
- 변하는 부분을 분리해서 타입 계층을 만들고, 변하지 않는 부분에 합성한다.

**Spring Boot 안티패턴**

❌ getter로 꺼내서 외부에서 처리 (Tell, Don't Ask 위반)
```java
if (order.getStatus().equals("PENDING") && order.getAmount() > 0) {
    order.setStatus("CONFIRMED");
}
```

✅ 객체에게 메시지를 보내 스스로 처리하게
```java
order.confirm();
```

**체크리스트**
- [ ] getter로 꺼낸 값을 외부에서 조작하지 않는가? (Tell, Don't Ask)
- [ ] 변경 가능한 내부 상태가 외부에 노출되지 않는가?
- [ ] 인터페이스만 보고 사용 가능한가?
- [ ] 변하는 부분(할인 정책, 배송 방식 등)이 별도 타입으로 분리되었는가?

---

### 3. 의존성 관리 (8·9장)

**핵심 원칙**
- 의존성은 단방향이어야 한다. 순환 의존성은 설계 실패 신호다.
- 구체 클래스가 아닌 **추상화(인터페이스/추상클래스)에 의존**한다.
- **`new`는 해롭다** — 내부에서 구체 클래스를 `new`로 생성하면 구체 클래스에 결합된다. 생성과 사용을 분리한다.
- 의존성은 생성자를 통해 명시적으로 드러낸다.
- 변경이 예상되는 지점에서는 컴파일/런타임 의존성을 분리한다.

**Spring Boot 안티패턴**

❌ Service 내부에서 구체 구현체를 직접 생성
```java
public class OrderService {
    private final DiscountPolicy policy = new RateDiscountPolicy();
}
```

✅ 생성자 주입으로 추상화에 의존
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final DiscountPolicy discountPolicy;
}
```

**체크리스트**
- [ ] 구체 클래스가 아닌 인터페이스에 의존하는가?
- [ ] `new`로 내부에서 구체 클래스를 생성하는 코드가 있는가?
- [ ] 의존성이 생성자를 통해 명시적으로 드러나는가?
- [ ] 순환 의존성이 없는가?
- [ ] 도메인 레이어가 인프라/프레임워크에 의존하지 않는가?

---

### 4. 합성 vs 상속 (10·11장)

**핵심 원칙**
- **코드 재사용 목적의 상속은 피한다.** 상속은 캡슐화를 깨고 강한 결합을 만든다.
- **합성(Composition)을 기본으로 선택한다.** 퍼블릭 인터페이스를 통해서만 협력한다.
- 상속을 사용하는 유일한 정당한 이유: **타입 계층 구성 (is-a 관계 + 행동 호환성)**
- `super` 참조를 사용하면 두 클래스가 강하게 결합된다.
- 상속 남용 시 클래스 폭발(combinatorial explosion) 발생.

**Spring Boot 안티패턴**

❌ 코드 재사용 목적의 상속
```java
public abstract class BaseService {
    protected UserRepository userRepository;
    protected void validateUser(Long userId) { ... }
}
public class OrderService extends BaseService {
    public void placeOrder(Long userId) { validateUser(userId); }
}
```

✅ 합성으로 공통 로직 재사용
```java
public class OrderService {
    private final UserValidator userValidator;
    public void placeOrder(Long userId) { userValidator.validate(userId); }
}
```

**체크리스트**
- [ ] 상속을 사용하는 이유가 "코드 재사용"인가? (그렇다면 합성으로 전환)
- [ ] `super`를 호출하는 코드가 있는가? (강한 결합 신호)
- [ ] 새 기능 추가 시 기존 클래스를 수정하지 않아도 되는가?
- [ ] 기능 조합이 늘어날 때 클래스 수가 폭발하지 않는가?

---

### 5. 타입 계층·LSP (13장)

**핵심 원칙**
- 상속의 일차 목적은 **타입 계층 구성**이다. 행동이 호환될 경우에만 타입 계층으로 묶는다.
- **리스코프 치환 원칙(LSP)**: 자식 클래스는 부모 클래스를 완전히 대체할 수 있어야 한다.
- is-a 관계가 성립해도 **행동이 호환되지 않으면 상속을 사용하지 않는다.**
- **개방-폐쇄 원칙(OCP)**: 기존 코드를 수정하지 않고 확장 가능해야 한다.
- 조건문(if/switch)으로 타입을 분기하는 코드는 다형성으로 대체를 검토한다.

**Spring Boot 안티패턴**

❌ OCP 위반 — 새 타입 추가 시 기존 코드 수정 필요
```java
public int discount(String type, int price) {
    if (type.equals("RATE")) return price * 10 / 100;
    if (type.equals("AMOUNT")) return 1000;
    // 새 정책마다 여기를 수정해야 함
}
```

✅ 추상화로 OCP 준수
```java
public interface DiscountPolicy {
    int discount(int price);
}
public class RateDiscountPolicy implements DiscountPolicy {
    public int discount(int price) { return price * 10 / 100; }
}
```

**체크리스트**
- [ ] 자식 클래스가 부모를 완전히 대체할 수 있는가? (LSP)
- [ ] 자식 클래스가 부모의 메서드 의미를 바꾸지 않는가?
- [ ] 새 타입/기능 추가 시 기존 코드를 수정하지 않아도 되는가? (OCP)
- [ ] 조건문으로 타입을 분기하는 코드가 있는가? (다형성으로 대체 검토)
