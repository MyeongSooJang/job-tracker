package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.ResumeDownload;
import com.jobtracker.application.service.dto.ResumeResponse;
import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeFileRepository resumeFileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResumeResponse upload(Long userId, String fileName, byte[] content) {
        ResumeFile resume = resumeFileRepository.findByUserId(userId)
                .map(existing -> {
                    existing.replace(fileName, content);
                    return existing;
                })
                .orElseGet(() -> resumeFileRepository.save(ResumeFile.upload(findUser(userId), fileName, content)));
        return ResumeResponse.from(resume);
    }

    public ResumeDownload download(Long userId) {
        ResumeFile resume = findResume(userId);
        return new ResumeDownload(resume.getFileName(), resume.getContent());
    }

    @Transactional
    public void delete(Long userId) {
        resumeFileRepository.delete(findResume(userId));
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
