# Job Tracker 백엔드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 기반 취업 활동 추적 백엔드 API 서버 구현 (회사, 지원 건, 면접, 일정 관리)

**Architecture:** DDD 레이어드 아키텍처 — presentation / application / domain / infrastructure 4계층. 도메인 객체가 비즈니스 로직(상태 전이 등)을 직접 담당하며 Service는 흐름 조율에만 집중한다. 모든 엔티티에 userId 컬럼을 미리 추가해 JWT 계정 확장을 준비한다.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, PostgreSQL, Gradle, Springdoc OpenAPI 2.6, Lombok, H2(테스트 전용)

## Global Constraints

- Java 21 (record, switch expression, text block 적극 활용)
- Spring Boot 3.3.5
- 패키지 루트: `com.jobtracker`
- 모든 엔티티에 `userId Long` 컬럼 포함 (현재는 단일 사용자, 추후 JWT 확장)
- 상태 전이 로직은 반드시 도메인 엔티티 내부에서 처리 (Service에서 getter로 꺼내 판단 금지)
- REST API 응답: 성공 200/201, 실패 4xx with `{ "code": "...", "message": "..." }`
- 테스트는 도메인 단위테스트 + Controller @WebMvcTest 두 가지로 구성
- 커밋 메시지는 한국어 (`feat: Company 도메인 구현`)

---

## 파일 구조

```
job-tracker/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/jobtracker/
│   │   │   ├── JobTrackerApplication.java
│   │   │   ├── global/
│   │   │   │   ├── exception/
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   └── response/
│   │   │   │       └── ErrorResponse.java
│   │   │   ├── presentation/controller/
│   │   │   │   ├── CompanyController.java
│   │   │   │   ├── ApplicationController.java
│   │   │   │   ├── InterviewController.java
│   │   │   │   └── ScheduleController.java
│   │   │   ├── application/service/
│   │   │   │   ├── CompanyService.java
│   │   │   │   ├── ApplicationService.java
│   │   │   │   ├── InterviewService.java
│   │   │   │   └── ScheduleService.java
│   │   │   ├── domain/
│   │   │   │   ├── company/
│   │   │   │   │   ├── Company.java
│   │   │   │   │   └── CompanyRepository.java
│   │   │   │   ├── application/
│   │   │   │   │   ├── Application.java
│   │   │   │   │   ├── ApplicationStatus.java
│   │   │   │   │   └── ApplicationRepository.java
│   │   │   │   ├── interview/
│   │   │   │   │   ├── Interview.java
│   │   │   │   │   ├── InterviewQuestion.java
│   │   │   │   │   └── InterviewRepository.java
│   │   │   │   └── schedule/
│   │   │   │       ├── Schedule.java
│   │   │   │       ├── ScheduleType.java
│   │   │   │       └── ScheduleRepository.java
│   │   │   └── infrastructure/persistence/
│   │   │       ├── CompanyJpaRepository.java
│   │   │       ├── ApplicationJpaRepository.java
│   │   │       ├── InterviewJpaRepository.java
│   │   │       └── ScheduleJpaRepository.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   └── test/java/com/jobtracker/
│       ├── domain/
│       │   ├── CompanyTest.java
│       │   ├── ApplicationTest.java
│       │   └── InterviewTest.java
│       └── presentation/
│           ├── CompanyControllerTest.java
│           ├── ApplicationControllerTest.java
│           ├── InterviewControllerTest.java
│           └── ScheduleControllerTest.java
```

---

## Task 1: 프로젝트 기본 설정

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-test.yml`
- Create: `src/main/java/com/jobtracker/JobTrackerApplication.java`
- Create: `src/main/java/com/jobtracker/global/exception/BusinessException.java`
- Create: `src/main/java/com/jobtracker/global/exception/ErrorCode.java`
- Create: `src/main/java/com/jobtracker/global/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/jobtracker/global/response/ErrorResponse.java`

**Interfaces:**
- Consumes: 없음
- Produces: `BusinessException(ErrorCode)`, `ErrorCode` enum, `GlobalExceptionHandler` — 모든 태스크에서 공통 사용

- [ ] **Step 1: build.gradle 작성**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.jobtracker'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: settings.gradle 작성**

```groovy
rootProject.name = 'job-tracker'
```

- [ ] **Step 3: application.yml 작성**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jobtracker
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  data:
    jpa:
      repositories:
        bootstrap-mode: deferred

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 4: application-test.yml 작성**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

- [ ] **Step 5: JobTrackerApplication.java 작성**

`src/main/java/com/jobtracker/JobTrackerApplication.java`

```java
package com.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JobTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobTrackerApplication.class, args);
    }
}
```

- [ ] **Step 6: ErrorCode enum 작성**

`src/main/java/com/jobtracker/global/exception/ErrorCode.java`

```java
package com.jobtracker.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원 건을 찾을 수 없습니다."),
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "면접을 찾을 수 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전이입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
```

- [ ] **Step 7: BusinessException 작성**

`src/main/java/com/jobtracker/global/exception/BusinessException.java`

```java
package com.jobtracker.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 8: ErrorResponse 작성**

`src/main/java/com/jobtracker/global/response/ErrorResponse.java`

```java
package com.jobtracker.global.response;

import com.jobtracker.global.exception.ErrorCode;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
```

- [ ] **Step 9: GlobalExceptionHandler 작성**

`src/main/java/com/jobtracker/global/exception/GlobalExceptionHandler.java`

```java
package com.jobtracker.global.exception;

import com.jobtracker.global.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }
}
```

- [ ] **Step 10: 애플리케이션 기동 확인**

```bash
./gradlew bootRun
```

Expected: `Started JobTrackerApplication` 로그 출력, http://localhost:8080/swagger-ui.html 접근 가능

- [ ] **Step 11: 커밋**

```bash
git init
git add .
git commit -m "feat: 프로젝트 초기 설정 및 공통 예외 처리 구성"
```

---

## Task 2: Company 도메인

**Files:**
- Create: `src/main/java/com/jobtracker/domain/company/Company.java`
- Create: `src/main/java/com/jobtracker/domain/company/CompanyRepository.java`
- Create: `src/main/java/com/jobtracker/infrastructure/persistence/CompanyJpaRepository.java`
- Create: `src/main/java/com/jobtracker/application/service/CompanyService.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/CompanyController.java`
- Test: `src/test/java/com/jobtracker/domain/CompanyTest.java`
- Test: `src/test/java/com/jobtracker/presentation/CompanyControllerTest.java`

**Interfaces:**
- Consumes: `BusinessException(ErrorCode)`, `ErrorCode.COMPANY_NOT_FOUND`
- Produces:
  - `Company.create(Long userId, String name, String industry, String website): Company`
  - `Company.update(String name, String industry, String website): void`
  - `CompanyRepository.findById(Long id): Optional<Company>`
  - `CompanyRepository.findAll(): List<Company>`

- [ ] **Step 1: Company 도메인 단위 테스트 작성**

`src/test/java/com/jobtracker/domain/CompanyTest.java`

```java
package com.jobtracker.domain;

import com.jobtracker.domain.company.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyTest {

    @Test
    void create_회사를_생성한다() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");

        assertThat(company.getName()).isEqualTo("카카오");
        assertThat(company.getIndustry()).isEqualTo("IT");
        assertThat(company.getWebsite()).isEqualTo("https://kakao.com");
        assertThat(company.getUserId()).isEqualTo(1L);
    }

    @Test
    void update_회사_정보를_수정한다() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        company.update("카카오뱅크", "핀테크", "https://kakaobank.com");

        assertThat(company.getName()).isEqualTo("카카오뱅크");
        assertThat(company.getIndustry()).isEqualTo("핀테크");
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.CompanyTest"
```

Expected: FAIL (Company 클래스 없음)

- [ ] **Step 3: Company 엔티티 작성**

`src/main/java/com/jobtracker/domain/company/Company.java`

```java
package com.jobtracker.domain.company;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String name;

    private String industry;

    private String website;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Company create(Long userId, String name, String industry, String website) {
        Company company = new Company();
        company.userId = userId;
        company.name = name;
        company.industry = industry;
        company.website = website;
        return company;
    }

    public void update(String name, String industry, String website) {
        this.name = name;
        this.industry = industry;
        this.website = website;
    }
}
```

- [ ] **Step 4: CompanyRepository 인터페이스 작성**

`src/main/java/com/jobtracker/domain/company/CompanyRepository.java`

```java
package com.jobtracker.domain.company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(Long id);
    List<Company> findAll();
}
```

- [ ] **Step 5: CompanyJpaRepository 작성**

`src/main/java/com/jobtracker/infrastructure/persistence/CompanyJpaRepository.java`

```java
package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<Company, Long>, CompanyRepository {
}
```

- [ ] **Step 6: 도메인 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.CompanyTest"
```

Expected: PASS

- [ ] **Step 7: CompanyController 테스트 작성**

`src/test/java/com/jobtracker/presentation/CompanyControllerTest.java`

```java
package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.domain.company.Company;
import com.jobtracker.presentation.controller.CompanyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean CompanyService companyService;

    @Test
    void POST_companies_회사를_등록한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        given(companyService.register(any())).willReturn(company);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "카카오", "industry", "IT", "website", "https://kakao.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("카카오"));
    }

    @Test
    void GET_companies_회사_목록을_조회한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        given(companyService.findAll()).willReturn(List.of(company));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("카카오"));
    }
}
```

- [ ] **Step 8: CompanyService 작성**

`src/main/java/com/jobtracker/application/service/CompanyService.java`

```java
package com.jobtracker.application.service;

import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.dto.CompanyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public Company register(CompanyRequest request) {
        return companyRepository.save(
                Company.create(1L, request.name(), request.industry(), request.website())
        );
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
```

- [ ] **Step 9: CompanyRequest DTO 작성**

`src/main/java/com/jobtracker/presentation/controller/dto/CompanyRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String industry,
        String website
) {}
```

- [ ] **Step 10: CompanyController 작성**

`src/main/java/com/jobtracker/presentation/controller/CompanyController.java`

```java
package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.CompanyService;
import com.jobtracker.domain.company.Company;
import com.jobtracker.presentation.controller.dto.CompanyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company register(@RequestBody @Valid CompanyRequest request) {
        return companyService.register(request);
    }

    @GetMapping
    public List<Company> findAll() {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    public Company findById(@PathVariable Long id) {
        return companyService.findById(id);
    }
}
```

- [ ] **Step 11: Controller 테스트 실행 — 통과 확인**

```bash
./gradlew test --tests "com.jobtracker.presentation.CompanyControllerTest"
```

Expected: PASS

- [ ] **Step 12: 커밋**

```bash
git add .
git commit -m "feat: Company 도메인 구현 (CRUD API)"
```

---

## Task 3: Application 도메인 (상태 전이 포함)

**Files:**
- Create: `src/main/java/com/jobtracker/domain/application/ApplicationStatus.java`
- Create: `src/main/java/com/jobtracker/domain/application/Application.java`
- Create: `src/main/java/com/jobtracker/domain/application/ApplicationRepository.java`
- Create: `src/main/java/com/jobtracker/infrastructure/persistence/ApplicationJpaRepository.java`
- Create: `src/main/java/com/jobtracker/application/service/ApplicationService.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/dto/ApplicationRequest.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/dto/ApplicationStatusRequest.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/ApplicationController.java`
- Test: `src/test/java/com/jobtracker/domain/ApplicationTest.java`
- Test: `src/test/java/com/jobtracker/presentation/ApplicationControllerTest.java`

**Interfaces:**
- Consumes: `Company.create(...)`, `CompanyRepository.findById(Long)`, `ErrorCode.APPLICATION_NOT_FOUND`, `ErrorCode.INVALID_STATUS_TRANSITION`
- Produces:
  - `Application.create(Long userId, Company company, String position, LocalDate appliedDate): Application`
  - `Application.advanceStatus(ApplicationStatus next): void` — 불가 시 `BusinessException(INVALID_STATUS_TRANSITION)` 던짐
  - `ApplicationRepository.findById(Long id): Optional<Application>`
  - `ApplicationRepository.findAll(): List<Application>`

- [ ] **Step 1: Application 도메인 단위 테스트 작성**

`src/test/java/com/jobtracker/domain/ApplicationTest.java`

```java
package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private Company company() {
        return Company.create(1L, "카카오", "IT", "https://kakao.com");
    }

    @Test
    void create_초기_상태는_RESUME_SUBMITTED이다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void advanceStatus_서류합격으로_전이한다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());
        app.advanceStatus(ApplicationStatus.RESUME_PASSED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_PASSED);
    }

    @Test
    void advanceStatus_유효하지_않은_전이시_예외를_던진다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());

        assertThatThrownBy(() -> app.advanceStatus(ApplicationStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void advanceStatus_불합격은_모든_진행_상태에서_가능하다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());
        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.ApplicationTest"
```

Expected: FAIL (Application 클래스 없음)

- [ ] **Step 3: ApplicationStatus enum 작성**

`src/main/java/com/jobtracker/domain/application/ApplicationStatus.java`

```java
package com.jobtracker.domain.application;

public enum ApplicationStatus {
    RESUME_SUBMITTED,
    RESUME_PASSED,
    INTERVIEW_1,
    INTERVIEW_2,
    FINAL,
    ACCEPTED,
    REJECTED;

    public boolean canAdvanceTo(ApplicationStatus next) {
        return switch (this) {
            case RESUME_SUBMITTED -> next == RESUME_PASSED || next == REJECTED;
            case RESUME_PASSED    -> next == INTERVIEW_1 || next == REJECTED;
            case INTERVIEW_1      -> next == INTERVIEW_2 || next == FINAL || next == REJECTED;
            case INTERVIEW_2      -> next == FINAL || next == REJECTED;
            case FINAL            -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED, REJECTED -> false;
        };
    }
}
```

- [ ] **Step 4: Application 엔티티 작성**

`src/main/java/com/jobtracker/domain/application/Application.java`

```java
package com.jobtracker.domain.application;

import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private LocalDate appliedDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Application create(Long userId, Company company, String position, LocalDate appliedDate) {
        Application application = new Application();
        application.userId = userId;
        application.company = company;
        application.position = position;
        application.status = ApplicationStatus.RESUME_SUBMITTED;
        application.appliedDate = appliedDate;
        return application;
    }

    public void advanceStatus(ApplicationStatus next) {
        if (!this.status.canAdvanceTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
    }
}
```

- [ ] **Step 5: ApplicationRepository 인터페이스 작성**

`src/main/java/com/jobtracker/domain/application/ApplicationRepository.java`

```java
package com.jobtracker.domain.application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Application save(Application application);
    Optional<Application> findById(Long id);
    List<Application> findAll();
}
```

- [ ] **Step 6: ApplicationJpaRepository 작성**

`src/main/java/com/jobtracker/infrastructure/persistence/ApplicationJpaRepository.java`

```java
package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long>, ApplicationRepository {
}
```

- [ ] **Step 7: 도메인 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.ApplicationTest"
```

Expected: PASS

- [ ] **Step 8: ApplicationController 테스트 작성**

`src/test/java/com/jobtracker/presentation/ApplicationControllerTest.java`

```java
package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.presentation.controller.ApplicationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ApplicationService applicationService;

    private Application sampleApplication() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void POST_applications_지원_건을_등록한다() throws Exception {
        given(applicationService.register(any())).willReturn(sampleApplication());

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("companyId", 1, "position", "백엔드 개발자", "appliedDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESUME_SUBMITTED"));
    }

    @Test
    void PATCH_applications_status_상태를_전이한다() throws Exception {
        Application app = sampleApplication();
        app.advanceStatus(ApplicationStatus.RESUME_PASSED);
        given(applicationService.advanceStatus(eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESUME_PASSED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESUME_PASSED"));
    }
}
```

- [ ] **Step 9: DTO 작성**

`src/main/java/com/jobtracker/presentation/controller/dto/ApplicationRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String position,
        @NotNull LocalDate appliedDate
) {}
```

`src/main/java/com/jobtracker/presentation/controller/dto/ApplicationStatusRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import com.jobtracker.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {}
```

- [ ] **Step 10: ApplicationService 작성**

`src/main/java/com/jobtracker/application/service/ApplicationService.java`

```java
package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.dto.ApplicationRequest;
import com.jobtracker.presentation.controller.dto.ApplicationStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CompanyService companyService;

    @Transactional
    public Application register(ApplicationRequest request) {
        Company company = companyService.findById(request.companyId());
        return applicationRepository.save(
                Application.create(1L, company, request.position(), request.appliedDate())
        );
    }

    public List<Application> findAll() {
        return applicationRepository.findAll();
    }

    public Application findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    @Transactional
    public Application advanceStatus(Long id, ApplicationStatusRequest request) {
        Application application = findById(id);
        application.advanceStatus(request.status());
        return application;
    }
}
```

- [ ] **Step 11: ApplicationController 작성**

`src/main/java/com/jobtracker/presentation/controller/ApplicationController.java`

```java
package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.presentation.controller.dto.ApplicationRequest;
import com.jobtracker.presentation.controller.dto.ApplicationStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Application register(@RequestBody @Valid ApplicationRequest request) {
        return applicationService.register(request);
    }

    @GetMapping
    public List<Application> findAll() {
        return applicationService.findAll();
    }

    @GetMapping("/{id}")
    public Application findById(@PathVariable Long id) {
        return applicationService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Application advanceStatus(@PathVariable Long id,
                                     @RequestBody @Valid ApplicationStatusRequest request) {
        return applicationService.advanceStatus(id, request);
    }
}
```

- [ ] **Step 12: 전체 테스트 실행 — 통과 확인**

```bash
./gradlew test
```

Expected: PASS (CompanyTest, ApplicationTest, CompanyControllerTest, ApplicationControllerTest)

- [ ] **Step 13: 커밋**

```bash
git add .
git commit -m "feat: Application 도메인 구현 (상태 전이 포함)"
```

---

## Task 4: Interview 도메인

**Files:**
- Create: `src/main/java/com/jobtracker/domain/interview/Interview.java`
- Create: `src/main/java/com/jobtracker/domain/interview/InterviewQuestion.java`
- Create: `src/main/java/com/jobtracker/domain/interview/InterviewRepository.java`
- Create: `src/main/java/com/jobtracker/infrastructure/persistence/InterviewJpaRepository.java`
- Create: `src/main/java/com/jobtracker/application/service/InterviewService.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/dto/InterviewRequest.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/dto/InterviewQuestionRequest.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/InterviewController.java`
- Test: `src/test/java/com/jobtracker/domain/InterviewTest.java`
- Test: `src/test/java/com/jobtracker/presentation/InterviewControllerTest.java`

**Interfaces:**
- Consumes: `Application.create(...)`, `ApplicationService.findById(Long)`, `ErrorCode.INTERVIEW_NOT_FOUND`
- Produces:
  - `Interview.create(Application application, int round, LocalDateTime interviewDate, String location, String note): Interview`
  - `Interview.addQuestion(String question, String answer, String memo): void`
  - `InterviewRepository.findByApplicationId(Long applicationId): List<Interview>`

- [ ] **Step 1: Interview 도메인 단위 테스트 작성**

`src/test/java/com/jobtracker/domain/InterviewTest.java`

```java
package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.interview.Interview;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewTest {

    private Application application() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void create_면접을_생성한다() {
        Interview interview = Interview.create(application(), 1, LocalDateTime.now(), "판교 본사", "1차 면접");

        assertThat(interview.getRound()).isEqualTo(1);
        assertThat(interview.getLocation()).isEqualTo("판교 본사");
    }

    @Test
    void addQuestion_질문을_추가한다() {
        Interview interview = Interview.create(application(), 1, LocalDateTime.now(), "판교 본사", "1차 면접");
        interview.addQuestion("자기소개 해주세요", "저는...", "잘 대답했음");

        assertThat(interview.getQuestions()).hasSize(1);
        assertThat(interview.getQuestions().get(0).getQuestion()).isEqualTo("자기소개 해주세요");
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.InterviewTest"
```

Expected: FAIL

- [ ] **Step 3: InterviewQuestion 엔티티 작성**

`src/main/java/com/jobtracker/domain/interview/InterviewQuestion.java`

```java
package com.jobtracker.domain.interview;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private String memo;

    static InterviewQuestion create(Interview interview, String question, String answer, String memo) {
        InterviewQuestion q = new InterviewQuestion();
        q.interview = interview;
        q.question = question;
        q.answer = answer;
        q.memo = memo;
        return q;
    }
}
```

- [ ] **Step 4: Interview 엔티티 작성**

`src/main/java/com/jobtracker/domain/interview/Interview.java`

```java
package com.jobtracker.domain.interview;

import com.jobtracker.domain.application.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    private int round;

    private LocalDateTime interviewDate;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Interview create(Application application, int round,
                                   LocalDateTime interviewDate, String location, String note) {
        Interview interview = new Interview();
        interview.application = application;
        interview.round = round;
        interview.interviewDate = interviewDate;
        interview.location = location;
        interview.note = note;
        return interview;
    }

    public void addQuestion(String question, String answer, String memo) {
        questions.add(InterviewQuestion.create(this, question, answer, memo));
    }

    public List<InterviewQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}
```

- [ ] **Step 5: InterviewRepository 인터페이스 작성**

`src/main/java/com/jobtracker/domain/interview/InterviewRepository.java`

```java
package com.jobtracker.domain.interview;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository {
    Interview save(Interview interview);
    Optional<Interview> findById(Long id);
    List<Interview> findByApplicationId(Long applicationId);
}
```

- [ ] **Step 6: InterviewJpaRepository 작성**

`src/main/java/com/jobtracker/infrastructure/persistence/InterviewJpaRepository.java`

```java
package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.interview.Interview;
import com.jobtracker.domain.interview.InterviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewJpaRepository extends JpaRepository<Interview, Long>, InterviewRepository {
    List<Interview> findByApplicationId(Long applicationId);
}
```

- [ ] **Step 7: 도메인 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.jobtracker.domain.InterviewTest"
```

Expected: PASS

- [ ] **Step 8: DTO 작성**

`src/main/java/com/jobtracker/presentation/controller/dto/InterviewRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull int round,
        @NotNull LocalDateTime interviewDate,
        String location,
        String note
) {}
```

`src/main/java/com/jobtracker/presentation/controller/dto/InterviewQuestionRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record InterviewQuestionRequest(
        @NotBlank String question,
        String answer,
        String memo
) {}
```

- [ ] **Step 9: InterviewService 작성**

`src/main/java/com/jobtracker/application/service/InterviewService.java`

```java
package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.domain.interview.InterviewRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.dto.InterviewQuestionRequest;
import com.jobtracker.presentation.controller.dto.InterviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;

    @Transactional
    public Interview register(Long applicationId, InterviewRequest request) {
        Application application = applicationService.findById(applicationId);
        return interviewRepository.save(
                Interview.create(application, request.round(), request.interviewDate(),
                        request.location(), request.note())
        );
    }

    public List<Interview> findByApplicationId(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Interview addQuestion(Long applicationId, Long interviewId, InterviewQuestionRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        interview.addQuestion(request.question(), request.answer(), request.memo());
        return interview;
    }
}
```

- [ ] **Step 10: InterviewController 작성**

`src/main/java/com/jobtracker/presentation/controller/InterviewController.java`

```java
package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.InterviewService;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.presentation.controller.dto.InterviewQuestionRequest;
import com.jobtracker.presentation.controller.dto.InterviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Interview register(@PathVariable Long applicationId,
                              @RequestBody @Valid InterviewRequest request) {
        return interviewService.register(applicationId, request);
    }

    @GetMapping
    public List<Interview> findAll(@PathVariable Long applicationId) {
        return interviewService.findByApplicationId(applicationId);
    }

    @PostMapping("/{interviewId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Interview addQuestion(@PathVariable Long applicationId,
                                 @PathVariable Long interviewId,
                                 @RequestBody @Valid InterviewQuestionRequest request) {
        return interviewService.addQuestion(applicationId, interviewId, request);
    }
}
```

- [ ] **Step 11: InterviewController 테스트 작성**

`src/test/java/com/jobtracker/presentation/InterviewControllerTest.java`

```java
package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.InterviewService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.presentation.controller.InterviewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean InterviewService interviewService;

    @Test
    void POST_interviews_면접을_등록한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Interview interview = Interview.create(app, 1, LocalDateTime.now(), "판교", "1차 면접");
        given(interviewService.register(eq(1L), any())).willReturn(interview);

        mockMvc.perform(post("/api/applications/1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("round", 1, "interviewDate", LocalDateTime.now().toString(),
                                        "location", "판교", "note", "1차 면접"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.round").value(1));
    }
}
```

- [ ] **Step 12: 전체 테스트 실행 — 통과 확인**

```bash
./gradlew test
```

Expected: PASS

- [ ] **Step 13: 커밋**

```bash
git add .
git commit -m "feat: Interview 도메인 구현 (질문/답변 포함)"
```

---

## Task 5: Schedule 도메인

**Files:**
- Create: `src/main/java/com/jobtracker/domain/schedule/ScheduleType.java`
- Create: `src/main/java/com/jobtracker/domain/schedule/Schedule.java`
- Create: `src/main/java/com/jobtracker/domain/schedule/ScheduleRepository.java`
- Create: `src/main/java/com/jobtracker/infrastructure/persistence/ScheduleJpaRepository.java`
- Create: `src/main/java/com/jobtracker/application/service/ScheduleService.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/dto/ScheduleRequest.java`
- Create: `src/main/java/com/jobtracker/presentation/controller/ScheduleController.java`
- Test: `src/test/java/com/jobtracker/presentation/ScheduleControllerTest.java`

**Interfaces:**
- Consumes: `ApplicationService.findById(Long)`, `ErrorCode.SCHEDULE_NOT_FOUND`
- Produces:
  - `Schedule.create(Application application, String title, LocalDateTime scheduledAt, ScheduleType type, String note): Schedule`
  - `ScheduleRepository.findByScheduledAtBetween(LocalDateTime from, LocalDateTime to): List<Schedule>`

- [ ] **Step 1: ScheduleType enum 작성**

`src/main/java/com/jobtracker/domain/schedule/ScheduleType.java`

```java
package com.jobtracker.domain.schedule;

public enum ScheduleType {
    INTERVIEW,
    DOCUMENT_SUBMISSION,
    TASK,
    OTHER
}
```

- [ ] **Step 2: Schedule 엔티티 작성**

`src/main/java/com/jobtracker/domain/schedule/Schedule.java`

```java
package com.jobtracker.domain.schedule;

import com.jobtracker.domain.application.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Schedule create(Application application, String title,
                                  LocalDateTime scheduledAt, ScheduleType type, String note) {
        Schedule schedule = new Schedule();
        schedule.application = application;
        schedule.title = title;
        schedule.scheduledAt = scheduledAt;
        schedule.type = type;
        schedule.note = note;
        return schedule;
    }
}
```

- [ ] **Step 3: ScheduleRepository 인터페이스 작성**

`src/main/java/com/jobtracker/domain/schedule/ScheduleRepository.java`

```java
package com.jobtracker.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(Long id);
    List<Schedule> findAll();
    List<Schedule> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);
}
```

- [ ] **Step 4: ScheduleJpaRepository 작성**

`src/main/java/com/jobtracker/infrastructure/persistence/ScheduleJpaRepository.java`

```java
package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleJpaRepository extends JpaRepository<Schedule, Long>, ScheduleRepository {
    List<Schedule> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);
}
```

- [ ] **Step 5: ScheduleRequest DTO 작성**

`src/main/java/com/jobtracker/presentation/controller/dto/ScheduleRequest.java`

```java
package com.jobtracker.presentation.controller.dto;

import com.jobtracker.domain.schedule.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleRequest(
        @NotNull Long applicationId,
        @NotBlank String title,
        @NotNull LocalDateTime scheduledAt,
        @NotNull ScheduleType type,
        String note
) {}
```

- [ ] **Step 6: ScheduleService 작성**

`src/main/java/com/jobtracker/application/service/ScheduleService.java`

```java
package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import com.jobtracker.presentation.controller.dto.ScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ApplicationService applicationService;

    @Transactional
    public Schedule register(ScheduleRequest request) {
        Application application = applicationService.findById(request.applicationId());
        return scheduleRepository.save(
                Schedule.create(application, request.title(), request.scheduledAt(),
                        request.type(), request.note())
        );
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> findByPeriod(LocalDateTime from, LocalDateTime to) {
        return scheduleRepository.findByScheduledAtBetween(from, to);
    }
}
```

- [ ] **Step 7: ScheduleController 작성**

`src/main/java/com/jobtracker/presentation/controller/ScheduleController.java`

```java
package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ScheduleService;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.presentation.controller.dto.ScheduleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Schedule register(@RequestBody @Valid ScheduleRequest request) {
        return scheduleService.register(request);
    }

    @GetMapping
    public List<Schedule> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from != null && to != null) {
            return scheduleService.findByPeriod(from, to);
        }
        return scheduleService.findAll();
    }
}
```

- [ ] **Step 8: ScheduleController 테스트 작성**

`src/test/java/com/jobtracker/presentation/ScheduleControllerTest.java`

```java
package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.ScheduleService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleType;
import com.jobtracker.presentation.controller.ScheduleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ScheduleService scheduleService;

    @Test
    void POST_schedules_일정을_등록한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Schedule schedule = Schedule.create(app, "1차 면접", LocalDateTime.now(), ScheduleType.INTERVIEW, "");
        given(scheduleService.register(any())).willReturn(schedule);

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicationId", 1,
                                "title", "1차 면접",
                                "scheduledAt", LocalDateTime.now().toString(),
                                "type", "INTERVIEW"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("1차 면접"));
    }

    @Test
    void GET_schedules_일정_목록을_조회한다() throws Exception {
        given(scheduleService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 9: 전체 테스트 실행 — 통과 확인**

```bash
./gradlew test
```

Expected: 전체 PASS

- [ ] **Step 10: 커밋**

```bash
git add .
git commit -m "feat: Schedule 도메인 구현 (날짜 범위 필터 포함)"
```

---

## Task 6: 통합 검증 및 Swagger 확인

**Files:**
- 수정 없음 (기존 설정으로 자동 생성)

**Interfaces:**
- Consumes: 모든 Controller 클래스
- Produces: Swagger UI에서 전체 API 문서 확인

- [ ] **Step 1: PostgreSQL 로컬 DB 생성**

```bash
createdb jobtracker
# 또는 psql에서:
# CREATE DATABASE jobtracker;
```

- [ ] **Step 2: 애플리케이션 기동**

```bash
./gradlew bootRun
```

Expected: `Started JobTrackerApplication` 출력

- [ ] **Step 3: Swagger UI 접속 확인**

브라우저에서 http://localhost:8080/swagger-ui.html 접속

Expected: Company, Application, Interview, Schedule 4개 섹션의 API 목록 확인

- [ ] **Step 4: 시나리오 테스트 (Swagger UI)**

순서대로 실행:
1. `POST /api/companies` — `{ "name": "카카오", "industry": "IT", "website": "https://kakao.com" }` → 201
2. `POST /api/applications` — `{ "companyId": 1, "position": "백엔드 개발자", "appliedDate": "2026-10-26" }` → 201, status: RESUME_SUBMITTED
3. `PATCH /api/applications/1/status` — `{ "status": "RESUME_PASSED" }` → 200, status: RESUME_PASSED
4. `POST /api/applications/1/interviews` — `{ "round": 1, "interviewDate": "2026-11-01T10:00:00", "location": "판교", "note": "1차" }` → 201
5. `POST /api/applications/1/interviews/1/questions` — `{ "question": "자기소개", "answer": "저는...", "memo": "잘 됨" }` → 201
6. `POST /api/schedules` — `{ "applicationId": 1, "title": "1차 면접", "scheduledAt": "2026-11-01T10:00:00", "type": "INTERVIEW" }` → 201
7. `GET /api/schedules` → 200, 일정 목록 반환 확인

- [ ] **Step 5: 최종 커밋**

```bash
git add .
git commit -m "feat: 백엔드 초기 구현 완료 - 전체 API 통합 검증"
```
