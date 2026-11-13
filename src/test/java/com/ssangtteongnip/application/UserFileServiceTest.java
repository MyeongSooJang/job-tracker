package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.UserFileService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserFileServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FILE_ID = 10L;

    @Mock UserFileRepository userFileRepository;
    @Mock UserRepository userRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private final User user = registeredUser();
    private UserFileService userFileService;

    private static User registeredUser() {
        User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    private UserFile storedFile(FileType type, String fileName) {
        return UserFile.upload(user, type, fileName, pdf(fileName), storage);
    }

    @BeforeEach
    void setUp() {
        userFileService = new UserFileService(userFileRepository, userRepository, storage);
    }

    @Test
    void 종류를_지정해_올리면_저장소에_올리고_저장한다() {
        byte[] content = pdf("portfolio");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userFileRepository.countByUserId(USER_ID)).willReturn(0L);
        given(userFileRepository.save(any(UserFile.class))).willAnswer(i -> i.getArgument(0));

        UserFileResponse response = userFileService.upload(USER_ID, FileType.PORTFOLIO, "포트폴리오.pdf", content);

        assertThat(response.type()).isEqualTo(FileType.PORTFOLIO);
        assertThat(response.fileName()).isEqualTo("포트폴리오.pdf");
        assertThat(response.sizeBytes()).isEqualTo(content.length);
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 한도_바로_아래면_올릴_수_있다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userFileRepository.countByUserId(USER_ID)).willReturn((long) UserFile.MAX_FILES_PER_USER - 1);
        given(userFileRepository.save(any(UserFile.class))).willAnswer(i -> i.getArgument(0));

        userFileService.upload(USER_ID, FileType.RESUME, "이력서.pdf", pdf("resume"));

        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 다른_요청이_이미_한도를_채웠다면_올바른_파일이어도_거절하고_저장소에도_올리지_않는다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userFileRepository.countByUserId(USER_ID)).willReturn((long) UserFile.MAX_FILES_PER_USER);

        assertErrorCode(() -> userFileService.upload(USER_ID, FileType.RESUME, "이력서.pdf", pdf("resume")),
                ErrorCode.FILE_LIMIT_EXCEEDED);

        verify(userFileRepository, never()).save(any());
        assertThat(storage.size()).isZero();
    }

    @Test
    void 잘못된_파일이면_저장하지_않는다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userFileRepository.countByUserId(USER_ID)).willReturn(0L);

        assertErrorCode(() -> userFileService.upload(USER_ID, FileType.RESUME, "a.pdf",
                "not pdf".getBytes(StandardCharsets.US_ASCII)), ErrorCode.INVALID_FILE_TYPE);

        verify(userFileRepository, never()).save(any());
    }

    @Test
    void 가입하지_않은_사용자는_올릴_수_없다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userFileService.upload(USER_ID, FileType.RESUME, "a.pdf", pdf("x")),
                ErrorCode.USER_NOT_FOUND);

        verify(userFileRepository, never()).save(any());
        assertThat(storage.size()).isZero();
    }

    @Test
    void 목록은_저장소가_준_순서_그대로_종류와_함께_내려준다() {
        UserFile newer = storedFile(FileType.PORTFOLIO, "새것.pdf");
        UserFile older = storedFile(FileType.RESUME, "옛것.pdf");
        given(userFileRepository.findUserFilesNewestFirst(USER_ID)).willReturn(List.of(newer, older));

        List<UserFileResponse> responses = userFileService.list(USER_ID);

        assertThat(responses).extracting(UserFileResponse::fileName).containsExactly("새것.pdf", "옛것.pdf");
        assertThat(responses).extracting(UserFileResponse::type)
                .containsExactly(FileType.PORTFOLIO, FileType.RESUME);
    }

    @Test
    void 올린_파일은_파일명과_내려받기_주소로_내려받을_수_있다() {
        UserFile file = storedFile(FileType.CERTIFICATE, "자격증.pdf");
        given(userFileRepository.findByIdAndUserId(FILE_ID, USER_ID)).willReturn(Optional.of(file));

        FileDownload download = userFileService.download(USER_ID, FILE_ID);

        assertThat(download.fileName()).isEqualTo("자격증.pdf");
        assertThat(download.url()).contains(file.getObjectKey());
        assertThat(download.expiresAt()).isEqualTo(FakeFileStorage.EXPIRES_AT);
    }

    @Test
    void 내_파일이_아니거나_없으면_내려받을_수_없다() {
        given(userFileRepository.findByIdAndUserId(FILE_ID, USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userFileService.download(USER_ID, FILE_ID), ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 파일을_삭제하면_저장소에서_제거되고_객체도_지워진다() {
        UserFile file = storedFile(FileType.RESUME, "이력서.pdf");
        String key = file.getObjectKey();
        given(userFileRepository.findByIdAndUserId(FILE_ID, USER_ID)).willReturn(Optional.of(file));

        userFileService.delete(USER_ID, FILE_ID);

        verify(userFileRepository).delete(file);
        assertThat(storage.contains(key)).isFalse();
    }

    @Test
    void 객체를_지우지_못해도_삭제는_성공한다() {
        UserFile file = storedFile(FileType.RESUME, "이력서.pdf");
        given(userFileRepository.findByIdAndUserId(FILE_ID, USER_ID)).willReturn(Optional.of(file));
        storage.failOnDelete();

        userFileService.delete(USER_ID, FILE_ID);

        verify(userFileRepository).delete(file);
    }

    @Test
    void 내_파일이_아니거나_없으면_삭제할_수_없다() {
        given(userFileRepository.findByIdAndUserId(FILE_ID, USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userFileService.delete(USER_ID, FILE_ID), ErrorCode.FILE_NOT_FOUND);
        verify(userFileRepository, never()).delete(any());
    }
}
