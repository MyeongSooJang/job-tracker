package com.ssangtteongnip.domain.profile;

public interface FileStorage {
    void put(String key, byte[] content, String contentType);
    void delete(String key);
    PresignedUrl presignedDownloadUrl(String key, String fileName);
    // 내려받지 않고 브라우저가 바로 보여 주는(inline) 짧은 유효기간의 주소.
    PresignedUrl presignedViewUrl(String key);
}
