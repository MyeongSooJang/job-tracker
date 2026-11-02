package com.jobtracker.application;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.ScheduleService;
import com.jobtracker.application.service.dto.ScheduleAddressRequest;
import com.jobtracker.application.service.dto.ScheduleRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import com.jobtracker.domain.schedule.ScheduleType;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock ApplicationService applicationService;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks ScheduleService scheduleService;

    private ScheduleRequest request(ScheduleType type) {
        return new ScheduleRequest(1L, "1차 면접", LocalDateTime.now(), type, "판교", "");
    }

    private void givenSavedSchedule() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        given(applicationService.findById(1L, 1L)).willReturn(app);
        given(scheduleRepository.save(any(Schedule.class))).willAnswer(i -> i.getArgument(0));
    }

    @Test
    void register_면접_일정이_생성되면_후기를_자동_생성한다() {
        givenSavedSchedule();
        given(reviewRepository.existsByApplicationId(any())).willReturn(false);

        scheduleService.register(1L, request(ScheduleType.INTERVIEW));

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void register_면접_일정이_생성되면_서류접수에서_면접_상태로_전환한다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        given(applicationService.findById(1L, 1L)).willReturn(app);
        given(scheduleRepository.save(any(Schedule.class))).willAnswer(i -> i.getArgument(0));
        given(reviewRepository.existsByApplicationId(any())).willReturn(false);

        scheduleService.register(1L, request(ScheduleType.INTERVIEW));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void register_면접이_아닌_일정은_상태를_바꾸지_않는다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        given(applicationService.findById(1L, 1L)).willReturn(app);
        given(scheduleRepository.save(any(Schedule.class))).willAnswer(i -> i.getArgument(0));

        scheduleService.register(1L, request(ScheduleType.TASK));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void register_이미_후기가_있으면_다시_생성하지_않는다() {
        givenSavedSchedule();
        given(reviewRepository.existsByApplicationId(any())).willReturn(true);

        scheduleService.register(1L, request(ScheduleType.INTERVIEW));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void register_면접이_아닌_일정은_후기를_생성하지_않는다() {
        givenSavedSchedule();

        scheduleService.register(1L, request(ScheduleType.TASK));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    private Schedule 판교_일정() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
        return Schedule.create(app, "1차 면접", LocalDateTime.of(2026, 11, 10, 14, 0), ScheduleType.INTERVIEW, "판교", "");
    }

    @Test
    void 소유자는_일정의_주소를_수정할_수_있다() {
        Schedule schedule = 판교_일정();
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

        scheduleService.updateAddress(1L, 1L, new ScheduleAddressRequest("서울"));

        assertThat(schedule.getAddress()).isEqualTo("서울");
    }

    @Test
    void 다른_사용자의_일정은_주소를_수정할_수_없다() {
        Schedule schedule = 판교_일정();
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

        assertErrorCode(() -> scheduleService.updateAddress(2L, 1L, new ScheduleAddressRequest("서울")),
                ErrorCode.SCHEDULE_NOT_FOUND);
        assertThat(schedule.getAddress()).isEqualTo("판교");
    }

    @Test
    void 존재하지_않는_일정의_주소는_수정할_수_없다() {
        given(scheduleRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(() -> scheduleService.updateAddress(1L, 1L, new ScheduleAddressRequest("서울")),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }
}
