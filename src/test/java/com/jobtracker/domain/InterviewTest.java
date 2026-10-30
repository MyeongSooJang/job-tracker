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
        Company company = Company.create("카카오", "IT", "https://kakao.com");
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
