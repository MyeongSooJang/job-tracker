# Task 2 Fix Report — Company Domain

**Date:** 2026-10-26

## Fixes Applied

### Important 1: application → presentation dependency violation
- Created `src/main/java/com/jobtracker/application/service/dto/CompanyRequest.java` with identical content.
- Updated `CompanyService` import from `com.jobtracker.presentation.controller.dto.CompanyRequest` to `com.jobtracker.application.service.dto.CompanyRequest`.
- Updated `CompanyController` import to use the new path.
- Old file `src/main/java/com/jobtracker/presentation/controller/dto/CompanyRequest.java` removed.

### Important 2: userId hardcoded as 1L
- Added `// TODO: replace with authenticated userId when JWT auth is added` comment above `Company.create(1L, ...)` in `CompanyService.register()`.

### Minor: GET /api/companies/{id} not tested
- Added `GET_companies_id_회사를_단건_조회한다` test: GET /api/companies/1 → 200 with `$.name = "카카오"`.
- Added `GET_companies_id_존재하지_않으면_404를_반환한다` test: GET /api/companies/1 when not found → 404 with `$.code = "COMPANY_NOT_FOUND"`.

### Minor: CompanyTest missing website assertion
- Added `assertThat(company.getWebsite()).isEqualTo("https://kakaobank.com")` to `update_회사_정보를_수정한다`.

## Test Results
- 6 tests total (2 domain, 4 controller) — all passed, 0 failures, 0 skipped.
