package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.ProfileService;
import com.ssangtteongnip.application.service.dto.ProfileRequest;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock ProfileRepository profileRepository;
    @Mock UserFileRepository userFileRepository;
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
    void 파일이_없으면_빈_파일_목록으로_프로필을_조회한다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(userFileRepository.findUserFilesNewestFirst(USER_ID)).willReturn(List.of());

        ProfileResponse response = profileService.getMyProfile(USER_ID);

        assertThat(response.email()).isEqualTo("hong@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.files()).isEmpty();
    }

    @Test
    void 파일이_있으면_프로필과_함께_종류가_붙은_파일_목록을_조회한다() {
        Profile profile = profile();
        FakeFileStorage storage = new FakeFileStorage();
        byte[] content = "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII);
        UserFile portfolio = UserFile.upload(profile.getUser(), FileType.PORTFOLIO, "포트폴리오.pdf", content, storage);
        UserFile resume = UserFile.upload(profile.getUser(), FileType.RESUME, "이력서.pdf", content, storage);
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
        given(userFileRepository.findUserFilesNewestFirst(USER_ID)).willReturn(List.of(portfolio, resume));

        ProfileResponse response = profileService.getMyProfile(USER_ID);

        assertThat(response.files()).extracting(UserFileResponse::fileName)
                .containsExactly("포트폴리오.pdf", "이력서.pdf");
        assertThat(response.files()).extracting(UserFileResponse::type)
                .containsExactly(FileType.PORTFOLIO, FileType.RESUME);
        assertThat(response.files().get(0).sizeBytes()).isEqualTo(content.length);
    }

    @Test
    void 프로필이_없으면_조회할_수_없다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> profileService.getMyProfile(USER_ID), ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    void 프로필을_수정하면_이름과_인적_사항이_바뀐_응답을_돌려준다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(userFileRepository.findUserFilesNewestFirst(USER_ID)).willReturn(List.of());

        ProfileResponse response = profileService.reviseMyProfile(USER_ID, request("홍길순"));

        assertThat(response.name()).isEqualTo("홍길순");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.desiredJob()).isEqualTo("백엔드 개발자");
        assertThat(response.jobSearchStartDate()).isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void 프로필이_없으면_수정할_수_없다() {
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> profileService.reviseMyProfile(USER_ID, request("홍길순")), ErrorCode.PROFILE_NOT_FOUND);
    }
}
