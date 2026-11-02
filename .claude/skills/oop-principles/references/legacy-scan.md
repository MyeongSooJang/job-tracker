# 사전 스캔과 레거시 수정 규칙

기존 코드를 수정하거나 기능을 추가할 때 코드 작성 **전에** 따르는 절차다.

## 수정 전 사전 스캔 절차

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

