---
name: making-method
description: Use when creating, renaming, or changing a Java method or its signature in this project ("메서드 만들어", "메서드 이름", "네이밍", "정적 팩토리", "of/from", "파라미터", "리팩터링"), when about to name a method of/from/get/process/handle, or when a call has a long expression nested inside its arguments. Defines 메서드 이름 짓기와 파라미터·호출부 가독성 기준.
---

# 메서드 만들기

메서드는 **처음 보는 사람이 호출부만 읽고 무슨 일을 하는지 알 수 있게** 짓는다. 이름과 인자가 설명이 되도록 만드는 것이 목표다. `oop-principles`가 "어떤 객체가 책임지는가"를 정한다면, 이 스킬은 그 책임을 **어떻게 부를 것인가**를 정한다. 메서드를 새로 짓거나 이름·시그니처를 바꿀 때 `oop-principles`와 함께 호출한다.

## 이름 짓기 절차

1. **하는 일을 한 문장으로 쓴다.** "회사의 평점을 찾는다", "회사 정보에 평점을 붙여 응답을 만든다".
2. **그 문장에서 동사와 대상을 뽑아 이름으로 쓴다.** `findCompanyRating`, `attachRating`.
3. **호출부를 소리 내어 읽어 본다.** `ratings.findCompanyRating(company)`가 문장처럼 읽히는지 본다.
4. **이름이 실제 동작과 같은지 확인한다.** 한 메서드가 하는 일이 이름보다 많거나 적으면 이름이나 메서드를 고친다.

## 규칙

### 1. 동사로 시작하고, 하는 일이 드러나게 한다

| 나쁨 | 좋음 | 이유 |
|---|---|---|
| `ratingOf(companyId)` | `findCompanyRating(company)` | 명사형은 동작이 안 보이고, 무엇의 평점인지도 모호하다 |
| `process`, `handle`, `doIt` | `changeStatus`, `reflectToApplication` | 무엇을 처리하는지 알 수 없다 |

- 조회 동사는 이 프로젝트 관례대로 **`find`**를 쓴다 (`search`, `fetch`, `get` 대신). `Optional`이 아니라 항상 값을 주는 조회라면 클래스 주석에 "없으면 어떤 값을 주는지" 적는다.
- 판단은 `is/has/can`, 변경은 의도를 드러내는 동사(`changeMemo`, `changeStatus`)를 쓴다. `setX`는 쓰지 않는다.
- **일상적인 단어를 쓴다.** `advance`, `proceed`, `commence`처럼 뜻을 한 번 더 해석해야 하는 단어 대신 `change`, `start`, `move`처럼 처음 보는 사람이 바로 이해하는 단어를 쓴다. 같은 동작을 가리키는 동사가 `advance`와 `move`로 갈라지지 않게 한 단어로 통일한다.

### 2. `of`/`from`처럼 무엇을 하는지 모호한 이름을 새로 만들지 않는다

- 값을 담아 객체를 만들 뿐이라면 **정적 팩토리를 두지 않고 생성자(`new`)**를 쓴다. `new`가 이미 "만든다"는 뜻이다.
- 엔티티는 CLAUDE.md 관례대로 `create(...)` 정적 팩토리 + `protected` 기본 생성자를 쓴다. 이 규칙은 엔티티에만 적용한다.
- 변환·결합처럼 **하는 일이 있다면** 그 일을 이름에 쓴다. 예: `CompanyResponse.of(company, rating)` 대신 `CompanyResponse.attachRating(company, rating)`.
- 이미 있는 `of`/`from`은 요청 범위 밖이면 고치지 않는다. 새로 만들거나 그 메서드를 수정할 때만 바꾸고 한 줄로 고지한다.

### 3. 이름이 대상을 말해야 한다. 단, 인자가 말해 주는 정보는 반복하지 않는다

- 인자가 말해 주는 정보를 이름에 되풀이하지 않는다. `changeStatusTo(next)`의 `To`는 `next`가 이미 말해 주므로 `changeStatus(next)`로 쓴다. `To`, `By` 같은 전치사는 인자 타입만으로 대상이 안 드러날 때(`findWrittenByCompanyId(Long)`)만 붙인다.

- 인자가 `Long id`라서 무엇의 값인지 안 드러나면, **도메인 객체를 받거나 이름에 대상을 넣는다.** `ratingOf(Long id)`보다 `findCompanyRating(Company company)`가 낫다.
- 도메인 객체를 받으면 호출부에서 `company.getId()`를 꺼낼 필요가 없어 Tell, Don't Ask에도 맞는다.
- 모호한 명사(`Summary`, `Info`, `Data`, `Manager`, `Helper`)를 클래스·메서드 이름에 쓰지 않는다. 무엇의 요약인지 드러나게 내용을 이름에 쓴다. (`CompanyReviewSummary` → `CompanyReviewRating`)

### 4. 인자 자리에 긴 식을 넣지 않는다

호출 인자에 식을 겹쳐 넣으면 무엇이 들어가는지 안 보인다. 단계마다 **이름 붙은 변수**로 풀어서 위에서 아래로 읽히게 한다.

```java
// 나쁨: 인자 안에 식이 겹쳐 있다
.map(company -> CompanyResponse.of(company,
        summaries.getOrDefault(company.getId(), CompanyReviewSummary.none(company.getId()))))

// 좋음: 변수 이름이 인자의 의미를 말한다
.map(company -> {
    CompanyReviewRating rating = reviewRatings.findCompanyRating(company);
    return CompanyResponse.attachRating(company, rating);
})
```

- 한 줄 람다가 두 줄 이상이 되면 `private` 메서드로 빼서 이름을 붙이는 것도 방법이다.
- 변수가 한 번만 쓰이고 이름이 새 정보를 주지 않으면(`result`, `temp`) 만들지 않는다.

### 5. 판단하는 메서드는 판단만, 바꾸는 메서드는 바꾸기만 한다

한 메서드는 한 가지 목적만 수행한다. 이름에 "그리고"가 필요하면 메서드를 쪼갠다. 판단도 하고 변경도 하면 이름이 거짓말이 된다.

| 목적 | 이름 | 하는 일 |
|---|---|---|
| 판단 | `canChangeStatus(next)` | true/false만 답한다. 상태를 바꾸지 않는다 |
| 검사 | `validateStatusChange(next)` | 안 되면 예외를 던진다. 상태를 바꾸지 않는다 |
| 변경 | 대입 한 줄 (`this.status = next;`) | 값만 바꾼다. 판단은 하지 않는다 |

```java
// 나쁨: 검사와 변경을 한 메서드가 함께 한다 (이름 moveTo에는 검사한다는 게 안 드러남)
public void moveTo(ApplicationStatus next) { if (!status.canAdvanceTo(next)) throw ...; this.status = next; }

// 좋음: 검사는 이름 있는 메서드로, 변경은 대입 한 줄로. 호출부에서 순서대로 읽힌다
public void accept() {
    validateStatusChange(ACCEPTED);
    this.status = ACCEPTED;
}
```

- **private 한 줄 대입 헬퍼는 만들지 않는다.** `changeStatus(next) { this.status = next; }`처럼 대입만 하는 메서드는 이름이 새 정보를 주지 않으므로 대입을 그대로 쓴다. 이름이 규칙이나 의도를 설명하는 경우(`validateStatusChange`)만 메서드로 뺀다.
- 예외: **외부에서 부르는 public 변경 메서드**(`changeMemo(memo)`)는 setter를 금지한 대신 쓰는 통로이고 검증이 붙을 자리이므로 한 줄 대입이어도 둔다.

- 조건이 안 맞을 때 **조용히 아무것도 안 하는 변경 메서드**를 만들지 않는다. 호출한 쪽이 바뀌었는지 알 수 없다. 불가능하면 예외를 던지거나, 판단 메서드를 따로 둔다.
- 판단 메서드를 서비스에서 이어 부르면 서비스에 도메인 로직이 생긴다. 판단과 변경을 잇는 일은 도메인 메서드 안에서 한다.

## 완료 체크리스트

- [ ] 모든 새 메서드 이름이 동사로 시작한다
- [ ] 호출부를 소리 내어 읽으면 문장처럼 읽힌다
- [ ] `of`/`from`을 새로 만들지 않았다 (엔티티 `create` 제외)
- [ ] 인자가 `Long id`뿐이라 대상이 안 보이는 곳이 없다
- [ ] 인자 안에 겹친 식이 없다
- [ ] 이름이 실제 동작과 일치한다 (이름보다 많은 일을 하지 않는다)
- [ ] 판단 메서드는 판단만, 변경 메서드는 변경만 한다 (조건 불충족 시 조용히 무시하지 않는다)
- [ ] 이름이 새 정보를 주지 않는 private 한 줄 대입 헬퍼를 만들지 않았다
- [ ] 이름에 인자가 이미 말해 주는 `To`/`By`가 붙어 있지 않다
- [ ] `advance`처럼 한 번 더 해석해야 하는 단어가 없다
- [ ] 기존 `of`/`from`·모호한 이름은 범위 밖이면 고치지 않고 보고만 했다

## 합리화 함정 — 이 생각이 들면 STOP

| 생각 | 현실 |
|---|---|
| "기존 코드가 다 `of`/`from`이니 맞추자" | 새로 만드는 메서드는 하는 일을 이름에 쓴다. 기존 것은 범위 밖이면 보고만 한다. |
| "짧은 게 읽기 좋다" | 짧아도 무슨 일인지 모르면 읽기 어렵다. 호출부를 읽어 보고 정한다. |
| "인자를 한 줄에 다 넣는 게 간결하다" | 간결함보다 인자의 의미가 보이는 것이 우선이다. 변수로 푼다. |
| "id만 받으면 호출이 단순하다" | 호출부가 `getId()`를 꺼내야 한다. 도메인 객체를 받는다. |
