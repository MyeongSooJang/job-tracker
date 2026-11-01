package com.jobtracker.application.service.dto;

import com.jobtracker.domain.profile.PersonalInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfileRequest(
        @NotBlank(message = "이름을 입력해주세요.") String name,
        @Size(max = 255, message = "연락처가 너무 깁니다.") String phone,
        LocalDate birthDate,
        @Size(max = 255, message = "주소가 너무 깁니다.") String address,
        @Size(max = 300, message = "GitHub 주소가 너무 깁니다.") String githubUrl,
        @Size(max = 300, message = "블로그 주소가 너무 깁니다.") String blogUrl,
        @Size(max = 300, message = "포트폴리오 주소가 너무 깁니다.") String portfolioUrl,
        @Size(max = 2000, message = "학력은 2000자 이하여야 합니다.") String education,
        @Size(max = 2000, message = "경력은 2000자 이하여야 합니다.") String career,
        @Size(max = 255, message = "희망 직무가 너무 깁니다.") String desiredJob,
        @Size(max = 2000, message = "자기소개는 2000자 이하여야 합니다.") String introduction) {

    public PersonalInfo toPersonalInfo() {
        return PersonalInfo.of(phone, birthDate, address, githubUrl, blogUrl, portfolioUrl,
                education, career, desiredJob, introduction);
    }
}
