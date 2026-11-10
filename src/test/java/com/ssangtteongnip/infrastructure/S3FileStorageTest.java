package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.config.StorageProperties;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.infrastructure.storage.S3FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    private static final StorageProperties PROPERTIES =
            new StorageProperties("test-bucket", "ap-northeast-2", Duration.ofMinutes(5));

    @Mock S3Client s3Client;

    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key")))
            .build();

    private S3FileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3FileStorage(s3Client, presigner, PROPERTIES);
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    void 올리면_설정한_버킷에_키와_콘텐츠_타입으로_내용을_저장한다() throws IOException {
        byte[] content = "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII);

        storage.put("resumes/1/a.pdf", content, "application/pdf");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo("resumes/1/a.pdf");
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(body.getValue().contentStreamProvider().newStream().readAllBytes()).isEqualTo(content);
    }

    @Test
    void 올리다_SDK_오류가_나면_저장소_오류로_바꿔_던진다() {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(SdkClientException.create("boom"));

        assertErrorCode(() -> storage.put("resumes/1/a.pdf", new byte[]{1}, "application/pdf"),
                ErrorCode.STORAGE_FAILED);
    }

    @Test
    void 지우면_설정한_버킷의_해당_키를_삭제한다() {
        storage.delete("resumes/1/a.pdf");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo("resumes/1/a.pdf");
    }

    @Test
    void 지우다_SDK_오류가_나면_저장소_오류로_바꿔_던진다() {
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(SdkClientException.create("boom"));

        assertErrorCode(() -> storage.delete("resumes/1/a.pdf"), ErrorCode.STORAGE_FAILED);
    }

    @Test
    void 보기_주소는_내려받지_않고_바로_보여_주도록_inline으로_서명한다() {
        PresignedUrl link = storage.presignedViewUrl("study-images/1/a.png");

        assertThat(link.url())
                .startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/study-images/1/a.png?")
                .contains("X-Amz-Expires=300")
                .contains("response-content-disposition=inline")
                .doesNotContain("attachment");
    }

    @Test
    void 내려받기_주소는_버킷과_키를_가리키고_설정한_만료_시간과_파일명을_담는다() {
        PresignedUrl link = storage.presignedDownloadUrl("resumes/1/a.pdf", "이력서.pdf");

        assertThat(link.url())
                .startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/resumes/1/a.pdf?")
                .contains("X-Amz-Expires=300")
                .contains("response-content-disposition=attachment");
        assertThat(link.expiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
    }
}
