package com.jobtracker.infrastructure.verification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobtracker.domain.user.MailSender;
import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

// Render 무료 플랜은 SMTP 포트(25/465/587)를 막으므로 HTTPS(443)인 Gmail API로 발송한다.
// 인증 메일은 쿨다운이 있어 드물게 나가므로 액세스 토큰은 캐시하지 않고 발송마다 리프레시 토큰으로 받는다.
@Slf4j
@Component
public class GmailMailSender implements MailSender {

    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String from;

    public GmailMailSender(RestClient.Builder restClientBuilder,
                           @Value("${app.mail.gmail.client-id}") String clientId,
                           @Value("${app.mail.gmail.client-secret}") String clientSecret,
                           @Value("${app.mail.gmail.refresh-token}") String refreshToken,
                           @Value("${app.mail.from}") String from) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String email, VerificationCode code) {
        try {
            String raw = encode(verificationMessage(email, code));
            restClient.post()
                    .uri(SEND_URL)
                    .header("Authorization", "Bearer " + fetchAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", raw))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | MessagingException | IOException e) {
            log.warn("인증 메일 발송 실패", e);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    private String fetchAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        TokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new RestClientException("액세스 토큰 응답이 비어 있습니다.");
        }
        return response.accessToken();
    }

    private MimeMessage verificationMessage(String email, VerificationCode code) throws MessagingException {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject("[Job Tracker] 이메일 인증 코드", StandardCharsets.UTF_8.name());
        message.setText("인증 코드: " + code.value() + "\n5분 안에 입력해주세요.", StandardCharsets.UTF_8.name());
        return message;
    }

    private String encode(MimeMessage message) throws MessagingException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return Base64.getUrlEncoder().encodeToString(out.toByteArray());
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
