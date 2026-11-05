package com.jobtracker.domain;

import com.jobtracker.domain.profile.FileStorage;
import com.jobtracker.domain.profile.PresignedUrl;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class FakeFileStorage implements FileStorage {

    public static final Instant EXPIRES_AT = Instant.parse("2026-11-05T01:05:00Z");

    private final Map<String, byte[]> objects = new HashMap<>();
    private boolean failOnPut;
    private boolean failOnDelete;

    public void failOnPut() {
        this.failOnPut = true;
    }

    public void failOnDelete() {
        this.failOnDelete = true;
    }

    public boolean contains(String key) {
        return objects.containsKey(key);
    }

    public byte[] contentOf(String key) {
        return objects.get(key);
    }

    public int size() {
        return objects.size();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        if (failOnPut) {
            throw new BusinessException(ErrorCode.STORAGE_FAILED);
        }
        objects.put(key, content);
    }

    @Override
    public void delete(String key) {
        if (failOnDelete) {
            throw new BusinessException(ErrorCode.STORAGE_FAILED);
        }
        objects.remove(key);
    }

    @Override
    public PresignedUrl presignedDownloadUrl(String key, String fileName) {
        return new PresignedUrl("https://fake-storage/" + key + "?fileName=" + fileName, EXPIRES_AT);
    }
}
