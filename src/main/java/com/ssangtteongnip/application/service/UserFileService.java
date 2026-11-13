package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFileService {

    private final UserFileRepository userFileRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    // 사용자 행을 먼저 잠가 같은 사용자의 동시 업로드를 직렬화한 뒤 개수를 센다.
    @Transactional
    public UserFileResponse upload(Long userId, FileType type, String fileName, byte[] content) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserFile.ensureCanAdd(userFileRepository.countByUserId(userId));
        UserFile file = userFileRepository.save(UserFile.upload(user, type, fileName, content, fileStorage));
        return UserFileResponse.describe(file);
    }

    public List<UserFileResponse> list(Long userId) {
        return userFileRepository.findUserFilesNewestFirst(userId).stream()
                .map(UserFileResponse::describe)
                .toList();
    }

    public FileDownload download(Long userId, Long fileId) {
        UserFile file = findFile(userId, fileId);
        PresignedUrl link = file.downloadUrl(fileStorage);
        return new FileDownload(link.url(), file.getFileName(), link.expiresAt());
    }

    @Transactional
    public void delete(Long userId, Long fileId) {
        UserFile file = findFile(userId, fileId);
        userFileRepository.delete(file);
        AfterCommit.runQuietly("파일 객체 삭제", () -> file.deleteFrom(fileStorage));
    }

    private UserFile findFile(Long userId, Long fileId) {
        return userFileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }
}
