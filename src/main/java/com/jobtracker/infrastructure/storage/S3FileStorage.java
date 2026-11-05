package com.jobtracker.infrastructure.storage;

import com.jobtracker.domain.profile.FileStorage;
import com.jobtracker.domain.profile.PresignedUrl;
import com.jobtracker.global.config.StorageProperties;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    @Override
    public void put(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw storageFailed("put", key, e);
        }
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw storageFailed("delete", key, e);
        }
    }

    @Override
    public PresignedUrl presignedDownloadUrl(String key, String fileName) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .responseContentDisposition(ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .build();
        try {
            PresignedGetObjectRequest presigned = presigner.presignGetObject(
                    presign -> presign.signatureDuration(properties.presignTtl()).getObjectRequest(request));
            return new PresignedUrl(presigned.url().toString(), presigned.expiration());
        } catch (SdkException e) {
            throw storageFailed("presign", key, e);
        }
    }

    private BusinessException storageFailed(String action, String key, SdkException cause) {
        log.error("S3 {} 실패: key={}", action, key, cause);
        return new BusinessException(ErrorCode.STORAGE_FAILED);
    }
}
