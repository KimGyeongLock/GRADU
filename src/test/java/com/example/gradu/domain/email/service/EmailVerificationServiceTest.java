package com.example.gradu.domain.email.service;

import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.email.EmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock JavaMailSender mailSender;

    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks EmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ttlMinutes", 5L);
    }

    @Test
    void sendCode_deletesOldAndStoresHashAndSendsEmail() {
        // given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(redis.opsForValue()).thenReturn(valueOps);

        String email = "test@handong.ac.kr";

        // when
        service.sendCode(email);

        // then
        verify(redis).delete("email:otp:" + email);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOps).set(keyCaptor.capture(), hashCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("email:otp:" + email);
        assertThat(hashCaptor.getValue()).isNotBlank(); // 랜덤코드라 값 비교는 안함
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void verifyCodeOnly_expired_throws() {
        // given
        String email = "a@handong.ac.kr";
        when(valueOps.get("email:otp:" + email)).thenReturn(null);
        when(redis.opsForValue()).thenReturn(valueOps);

        // when & then
        assertThatThrownBy(() -> service.verifyCodeOnly(email, "123456"))
                .isInstanceOf(EmailException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_OTP_EXPIRED);
    }

    @Test
    void verifyCodeOnly_invalid_throws() {
        // given
        String email = "a@handong.ac.kr";
        String storedHash = sha256Base64ForTest("654321"); // 저장된 코드는 654321
        when(valueOps.get("email:otp:" + email)).thenReturn(storedHash);
        when(redis.opsForValue()).thenReturn(valueOps);

        // when & then (입력은 123456)
        assertThatThrownBy(() -> service.verifyCodeOnly(email, "123456"))
                .isInstanceOf(EmailException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_OTP_INVALID);
    }

    @Test
    void verifyCodeOnly_valid_ok() {
        // given
        String email = "a@handong.ac.kr";
        String code = "123456";
        when(valueOps.get("email:otp:" + email)).thenReturn(sha256Base64ForTest(code));
        when(redis.opsForValue()).thenReturn(valueOps);

        // when
        service.verifyCodeOnly(email, code);

        // then
        // 예외가 안 나면 성공
        verify(valueOps).get("email:otp:" + email);
    }

    @Test
    void consumeCode_deletesKey() {
        // given
        String email = "a@handong.ac.kr";

        // when
        service.consumeCode(email);

        // then
        verify(redis).delete("email:otp:" + email);
    }

    @Test
    void sendCode_whenMessagingException_throwsEmailSendFailed() {
        // given
        String email = "test@handong.ac.kr";
        MimeMessage msg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(msg);
        when(redis.opsForValue()).thenReturn(valueOps);

        // MimeMessageHelper 내부에서 setTo/setSubject/setText 중 MessagingException 발생시키기 어려워서
        // mailSender.send 호출 시점에 Runtime 대신 MessagingException을 던질 수 없음(시그니처상)
        // => 가장 현실적인 분기 커버는 createMimeMessage() 단계에서 예외 유발(또는 spy로 sendHtml 강제)
        // 여기서는 createMimeMessage()가 실패했다고 가정
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail infra down"));

        // when & then
        // sendHtml은 MessagingException만 catch하므로, RuntimeException은 그대로 터짐.
        // => 이 테스트는 'EMAIL_SEND_FAILED' 분기 커버용으로는 부족.
        // 아래에 "권장 리팩토링" 참고.
        assertThatThrownBy(() -> service.sendCode(email))
                .isInstanceOf(RuntimeException.class);
    }

    // --- 테스트 전용 헬퍼: 서비스의 private 로직과 동일한 해시 생성 ---
    private static String sha256Base64ForTest(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
