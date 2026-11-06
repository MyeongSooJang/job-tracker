package com.jobtracker.infrastructure;

import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.infrastructure.verification.GmailMailSender;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GmailMailSenderTest {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private MockRestServiceServer server;
    private GmailMailSender mailSender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        mailSender = new GmailMailSender(builder, "client-id", "client-secret", "refresh-token", "noreply@example.com");
    }

    @Test
    void 리프레시_토큰으로_받은_액세스_토큰으로_인증_코드_메일을_발송한다() throws Exception {
        AtomicReference<String> sentBody = new AtomicReference<>();
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("client_id=client-id"),
                        org.hamcrest.Matchers.containsString("client_secret=client-secret"),
                        org.hamcrest.Matchers.containsString("refresh_token=refresh-token"),
                        org.hamcrest.Matchers.containsString("grant_type=refresh_token"))))
                .andRespond(withSuccess("{\"access_token\":\"access-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-123"))
                .andExpect(request -> sentBody.set(((MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456"));

        server.verify();
        MimeMessage sent = decode(sentBody.get());
        assertThat(sent.getFrom()[0].toString()).isEqualTo("noreply@example.com");
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("hong@example.com");
        assertThat(sent.getContent().toString()).contains("123456");
    }

    @Test
    void 액세스_토큰을_받지_못하면_메일_발송_실패로_변환한다() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertErrorCode(
                () -> mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456")),
                ErrorCode.MAIL_SEND_FAILED);
    }

    @Test
    void 액세스_토큰_응답이_비어있으면_메일_발송_실패로_변환한다() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertErrorCode(
                () -> mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456")),
                ErrorCode.MAIL_SEND_FAILED);
    }

    @Test
    void 발송_API가_실패하면_메일_발송_실패로_변환한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"access-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertErrorCode(
                () -> mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456")),
                ErrorCode.MAIL_SEND_FAILED);
    }

    // 요청 본문 {"raw":"<base64url MIME>"}에서 MIME 메시지를 복원한다.
    private MimeMessage decode(String requestJson) throws Exception {
        String raw = requestJson.replaceAll(".*\"raw\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        byte[] mime = Base64.getUrlDecoder().decode(raw);
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(mime));
    }
}
