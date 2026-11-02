package com.jobtracker.application;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.InterviewService;
import com.jobtracker.application.service.dto.InterviewQuestionRequest;
import com.jobtracker.application.service.dto.InterviewRequest;
import com.jobtracker.application.service.dto.InterviewScoreRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.domain.interview.InterviewRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long APPLICATION_ID = 10L;
    private static final Long OTHER_APPLICATION_ID = 99L;
    private static final Long INTERVIEW_ID = 100L;
    private static final LocalDateTime INTERVIEW_DATE = LocalDateTime.of(2026, 11, 10, 14, 0);

    @Mock InterviewRepository interviewRepository;
    @Mock ApplicationService applicationService;
    @InjectMocks InterviewService interviewService;

    private Application application;

    @BeforeEach
    void setUp() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        application = Application.create(USER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
    }

    private Interview interviewOf(Application owner) {
        return Interview.create(owner, 1, INTERVIEW_DATE, "판교 본사", "1차 면접");
    }

    private Application otherApplication() {
        Application other = Application.create(USER_ID, application.getCompany(), "프론트엔드 개발자", LocalDate.of(2026, 11, 2));
        ReflectionTestUtils.setField(other, "id", OTHER_APPLICATION_ID);
        return other;
    }

    @Test
    void 면접을_등록하면_지원_건에_연결되고_점수가_기록된다() {
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.save(any(Interview.class))).willAnswer(i -> i.getArgument(0));

        Interview registered = interviewService.register(USER_ID, APPLICATION_ID,
                new InterviewRequest(1, INTERVIEW_DATE, "판교 본사", "1차 면접", 80));

        assertThat(registered.getApplication()).isSameAs(application);
        assertThat(registered.getRound()).isEqualTo(1);
        assertThat(registered.getScore()).isEqualTo(80);
    }

    @Test
    void 다른_사용자의_지원_건에는_면접을_등록할_수_없다() {
        willThrow(new BusinessException(ErrorCode.APPLICATION_NOT_FOUND))
                .given(applicationService).findById(OTHER_USER_ID, APPLICATION_ID);

        assertErrorCode(() -> interviewService.register(OTHER_USER_ID, APPLICATION_ID,
                        new InterviewRequest(1, INTERVIEW_DATE, "판교 본사", "1차 면접", null)),
                ErrorCode.APPLICATION_NOT_FOUND);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void 면접에_점수를_매기면_점수가_저장된다() {
        Interview interview = interviewOf(application);
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.findById(INTERVIEW_ID)).willReturn(Optional.of(interview));

        interviewService.evaluate(USER_ID, APPLICATION_ID, INTERVIEW_ID, new InterviewScoreRequest(90));

        assertThat(interview.getScore()).isEqualTo(90);
    }

    @Test
    void 면접에_질문을_추가하면_질문이_기록된다() {
        Interview interview = interviewOf(application);
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.findById(INTERVIEW_ID)).willReturn(Optional.of(interview));

        interviewService.addQuestion(USER_ID, APPLICATION_ID, INTERVIEW_ID,
                new InterviewQuestionRequest("자기소개 해주세요", "저는...", "잘 대답했음"));

        assertThat(interview.getQuestions()).hasSize(1);
        assertThat(interview.getQuestions().get(0).getQuestion()).isEqualTo("자기소개 해주세요");
    }

    @Test
    void 존재하지_않는_면접은_평가할_수_없다() {
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.findById(INTERVIEW_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> interviewService.evaluate(USER_ID, APPLICATION_ID, INTERVIEW_ID, new InterviewScoreRequest(90)),
                ErrorCode.INTERVIEW_NOT_FOUND);
    }

    @Test
    void 다른_지원_건의_면접은_평가할_수_없다() {
        Interview otherInterview = interviewOf(otherApplication());
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.findById(INTERVIEW_ID)).willReturn(Optional.of(otherInterview));

        assertErrorCode(() -> interviewService.evaluate(USER_ID, APPLICATION_ID, INTERVIEW_ID, new InterviewScoreRequest(90)),
                ErrorCode.INTERVIEW_NOT_FOUND);
        assertThat(otherInterview.getScore()).isNull();
    }

    @Test
    void 다른_지원_건의_면접에는_질문을_추가할_수_없다() {
        Interview otherInterview = interviewOf(otherApplication());
        given(applicationService.findById(USER_ID, APPLICATION_ID)).willReturn(application);
        given(interviewRepository.findById(INTERVIEW_ID)).willReturn(Optional.of(otherInterview));

        assertErrorCode(() -> interviewService.addQuestion(USER_ID, APPLICATION_ID, INTERVIEW_ID,
                        new InterviewQuestionRequest("자기소개 해주세요", null, null)),
                ErrorCode.INTERVIEW_NOT_FOUND);
        assertThat(otherInterview.getQuestions()).isEmpty();
    }

    @Test
    void 다른_사용자의_지원_건에서는_면접_목록을_조회할_수_없다() {
        willThrow(new BusinessException(ErrorCode.APPLICATION_NOT_FOUND))
                .given(applicationService).findById(OTHER_USER_ID, APPLICATION_ID);

        assertErrorCode(() -> interviewService.findByApplicationId(OTHER_USER_ID, APPLICATION_ID),
                ErrorCode.APPLICATION_NOT_FOUND);
        verifyNoInteractions(interviewRepository);
    }
}
