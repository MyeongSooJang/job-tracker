package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.ResumeDownload;
import com.ssangtteongnip.application.service.dto.ResumeResponse;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.profile.ResumeFile;
import com.ssangtteongnip.domain.profile.ResumeFileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeFileRepository resumeFileRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Transactional
    public ResumeResponse upload(Long userId, String fileName, byte[] content) {
        ResumeFile resume = resumeFileRepository.findByUserId(userId)
                .map(existing -> replace(existing, fileName, content))
                .orElseGet(() -> resumeFileRepository.save(
                        ResumeFile.upload(findUser(userId), fileName, content, fileStorage)));
        return ResumeResponse.from(resume);
    }

    public ResumeDownload download(Long userId) {
        ResumeFile resume = findResume(userId);
        PresignedUrl link = resume.downloadUrl(fileStorage);
        return new ResumeDownload(link.url(), resume.getFileName(), link.expiresAt());
    }

    @Transactional
    public void delete(Long userId) {
        ResumeFile resume = findResume(userId);
        resumeFileRepository.delete(resume);
        AfterCommit.runQuietly("이력서 객체 삭제", () -> resume.deleteFrom(fileStorage));
    }

    private ResumeFile replace(ResumeFile resume, String fileName, byte[] content) {
        resume.replace(fileName, content, fileStorage)
                .ifPresent(previousKey ->
                        AfterCommit.runQuietly("이전 이력서 객체 삭제", () -> fileStorage.delete(previousKey)));
        return resume;
    }

    private ResumeFile findResume(Long userId) {
        return resumeFileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
