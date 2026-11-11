package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.CompanyRequest;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.CompanyReviewSummary;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks CompanyService companyService;

    private Application applicationOf(Company company) {
        return Application.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 10, 1));
    }

    private Review writtenReview(Application application, String content, int rating) {
        Review review = Review.create(application);
        review.write(content, rating);
        return review;
    }

    @Test
    void 새로운_회사는_입력한_정보로_등록된다() {
        given(companyRepository.existsByName("카카오")).willReturn(false);
        given(companyRepository.save(any(Company.class))).willAnswer(i -> i.getArgument(0));

        Company registered = companyService.register(new CompanyRequest("카카오", "IT", "https://kakao.com"));

        assertThat(registered.getName()).isEqualTo("카카오");
        assertThat(registered.getIndustry()).isEqualTo("IT");
        assertThat(registered.getWebsite()).isEqualTo("https://kakao.com");
    }

    @Test
    void 이미_등록된_회사는_다시_등록할_수_없다() {
        given(companyRepository.existsByName("카카오")).willReturn(true);

        assertErrorCode(
                () -> companyService.register(new CompanyRequest("카카오", "IT", "https://kakao.com")),
                ErrorCode.COMPANY_ALREADY_EXISTS);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void 존재하는_회사는_id로_조회할_수_있다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));

        assertThat(companyService.findById(1L)).isSameAs(company);
    }

    @Test
    void 존재하지_않는_회사는_조회할_수_없다() {
        given(companyRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(() -> companyService.findById(1L), ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    void 회사_목록은_회사별_후기_수와_평균_평점을_함께_준다() {
        Company kakao = Company.create("카카오", "IT", "https://kakao.com");
        Company naver = Company.create("네이버", "IT", "https://naver.com");
        ReflectionTestUtils.setField(kakao, "id", 1L);
        ReflectionTestUtils.setField(naver, "id", 2L);
        given(companyRepository.findAll()).willReturn(List.of(kakao, naver));
        given(reviewRepository.summarizeByCompany())
                .willReturn(List.of(new CompanyReviewSummary(1L, 2, 4.5)));

        List<CompanyResponse> responses = companyService.findAll();

        assertThat(responses).extracting(CompanyResponse::name, CompanyResponse::reviewCount,
                        CompanyResponse::averageRating)
                .containsExactly(
                        tuple("카카오", 2L, 4.5),
                        tuple("네이버", 0L, null));
    }

    @Test
    void 회사_상세는_후기_수와_평균_평점을_함께_준다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));
        given(reviewRepository.summarizeByCompanyId(1L))
                .willReturn(Optional.of(new CompanyReviewSummary(1L, 3, 4.0)));

        CompanyResponse response = companyService.findDetail(1L);

        assertThat(response.name()).isEqualTo("카카오");
        assertThat(response.reviewCount()).isEqualTo(3);
        assertThat(response.averageRating()).isEqualTo(4.0);
    }

    @Test
    void 후기가_없는_회사_상세는_평균_평점이_없다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));
        given(reviewRepository.summarizeByCompanyId(1L)).willReturn(Optional.empty());

        CompanyResponse response = companyService.findDetail(1L);

        assertThat(response.reviewCount()).isZero();
        assertThat(response.averageRating()).isNull();
    }

    @Test
    void 존재하지_않는_회사의_상세는_조회할_수_없다() {
        given(companyRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(() -> companyService.findDetail(1L), ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    void 회사의_후기는_직무와_지원_결과를_담아_작성자_정보_없이_준다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application application = applicationOf(company);
        application.startInterview(java.time.LocalDateTime.of(2026, 11, 5, 10, 0));
        application.advanceStatus(ApplicationStatus.ACCEPTED);
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));
        given(reviewRepository.findWrittenByCompanyId(1L))
                .willReturn(List.of(writtenReview(application, "면접관이 친절했어요", 5)));

        List<CompanyReviewResponse> reviews = companyService.findReviews(1L);

        assertThat(reviews).singleElement().satisfies(review -> {
            assertThat(review.rating()).isEqualTo(5);
            assertThat(review.content()).isEqualTo("면접관이 친절했어요");
            assertThat(review.position()).isEqualTo("백엔드 개발자");
            assertThat(review.stage()).isEqualTo(ApplicationStatus.ACCEPTED);
        });
    }

    @Test
    void 회사의_후기에는_면접_상세가_함께_담긴다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application application = applicationOf(company);
        Review review = writtenReview(application, "면접관이 친절했어요", 5);
        review.describeInterview(4, InterviewAtmosphere.TENSE, ApplyRoute.RECRUITER, List.of("자기소개", "프로젝트 경험"));
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));
        given(reviewRepository.findWrittenByCompanyId(1L)).willReturn(List.of(review));

        List<CompanyReviewResponse> reviews = companyService.findReviews(1L);

        assertThat(reviews).singleElement().satisfies(response -> {
            assertThat(response.difficulty()).isEqualTo(4);
            assertThat(response.atmosphere()).isEqualTo(InterviewAtmosphere.TENSE);
            assertThat(response.route()).isEqualTo(ApplyRoute.RECRUITER);
            assertThat(response.questions()).containsExactly("자기소개", "프로젝트 경험");
        });
    }

    @Test
    void 존재하지_않는_회사의_후기는_조회할_수_없다() {
        given(companyRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(() -> companyService.findReviews(1L), ErrorCode.COMPANY_NOT_FOUND);
    }
}
