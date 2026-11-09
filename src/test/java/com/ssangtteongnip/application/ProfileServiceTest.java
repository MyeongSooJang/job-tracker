package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.ProfileService;
import com.ssangtteongnip.application.service.dto.ProfileRequest;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.profile.ResumeFileRepository;
import com.ssangtteongnip.domain.profile.ResumeSummary;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock ProfileRepository profileRepository;
    @Mock ResumeFileRepository resumeFileRepository;
    @InjectMocks ProfileService profileService;

    private Profile profile() {
        User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());
        return Profile.create(user);
    }

    private ProfileRequest request(String name) {
        return new ProfileRequest(name, "010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", null, null, "OO대 컴퓨터공학", "인턴 6개월", "백엔드 개발자", "안녕하세요",
                LocalDate.of(2025, 3, 1));
    }

    @Test
    void 이력서가_없으면_이력서_정보_없이_프로필을_조회한다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(resumeFileRepository.findSummaryByUserId(USER_ID)).willReturn(Optional.empty());

        ProfileResponse response = profileService.getMyProfile(USER_ID);

        assertThat(response.email()).isEqualTo("hong@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.resume()).isNull();
    }

    @Test
    void 이력서가_있으면_프로필과_함께_이력서_정보를_조회한다() {
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 11, 1, 9, 0);
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(resumeFileRepository.findSummaryByUserId(USER_ID))
                .willReturn(Optional.of(new ResumeSummary("이력서.pdf", 1024L, uploadedAt)));

        ProfileResponse response = profileService.getMyProfile(USER_ID);

        assertThat(response.resume().fileName()).isEqualTo("이력서.pdf");
        assertThat(response.resume().sizeBytes()).isEqualTo(1024L);
        assertThat(response.resume().updatedAt()).isEqualTo(uploadedAt);
    }

    @Test
    void 프로필이_없으면_조회할_수_없다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> profileService.getMyProfile(USER_ID), ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    void 프로필을_수정하면_이름과_인적_사항이_바뀐_응답을_돌려준다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(resumeFileRepository.findSummaryByUserId(USER_ID)).willReturn(Optional.empty());

        ProfileResponse response = profileService.updateMyProfile(USER_ID, request("홍길순"));

        assertThat(response.name()).isEqualTo("홍길순");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.desiredJob()).isEqualTo("백엔드 개발자");
        assertThat(response.jobSearchStartDate()).isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void 프로필이_없으면_수정할_수_없다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> profileService.updateMyProfile(USER_ID, request("홍길순")), ErrorCode.PROFILE_NOT_FOUND);
    }
}
