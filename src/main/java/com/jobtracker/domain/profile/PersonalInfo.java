package com.jobtracker.domain.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalInfo {

    private String phone;
    private LocalDate birthDate;
    private String address;

    @Column(length = 300)
    private String githubUrl;
    @Column(length = 300)
    private String blogUrl;
    @Column(length = 300)
    private String portfolioUrl;

    @Column(length = 2000)
    private String education;
    @Column(length = 2000)
    private String career;

    private String desiredJob;
    @Column(length = 2000)
    private String introduction;

    public static PersonalInfo of(String phone, LocalDate birthDate, String address,
                                  String githubUrl, String blogUrl, String portfolioUrl,
                                  String education, String career,
                                  String desiredJob, String introduction) {
        PersonalInfo info = new PersonalInfo();
        info.phone = phone;
        info.birthDate = birthDate;
        info.address = address;
        info.githubUrl = githubUrl;
        info.blogUrl = blogUrl;
        info.portfolioUrl = portfolioUrl;
        info.education = education;
        info.career = career;
        info.desiredJob = desiredJob;
        info.introduction = introduction;
        return info;
    }
}
