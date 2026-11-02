package com.jobtracker.application.service.dto;

import com.jobtracker.domain.profile.PersonalInfo;
import com.jobtracker.domain.profile.Profile;

import java.time.LocalDate;

public record ProfileResponse(
        String email,
        String name,
        String phone,
        LocalDate birthDate,
        String address,
        String githubUrl,
        String blogUrl,
        String portfolioUrl,
        String education,
        String career,
        String desiredJob,
        String introduction,
        LocalDate jobSearchStartDate,
        ResumeResponse resume) {

    public static ProfileResponse of(Profile profile, ResumeResponse resume) {
        PersonalInfo info = profile.getInfo();
        return new ProfileResponse(
                profile.getUser().getEmail(),
                profile.getUser().getName(),
                info.getPhone(),
                info.getBirthDate(),
                info.getAddress(),
                info.getGithubUrl(),
                info.getBlogUrl(),
                info.getPortfolioUrl(),
                info.getEducation(),
                info.getCareer(),
                info.getDesiredJob(),
                info.getIntroduction(),
                info.getJobSearchStartDate(),
                resume);
    }
}
