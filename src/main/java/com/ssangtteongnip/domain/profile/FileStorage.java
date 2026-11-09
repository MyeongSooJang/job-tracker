package com.ssangtteongnip.domain.profile;

public interface FileStorage {
    void put(String key, byte[] content, String contentType);
    void delete(String key);
    PresignedUrl presignedDownloadUrl(String key, String fileName);
}
