package com.example.gradu.domain.email.service;

import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.email.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;

    @Value("${app.email.otp-ttl-minutes}")
    private long ttlMinutes;

    private static final SecureRandom RND = new SecureRandom();

    private static final String KEY_PREFIX = "email:otp:";

    public void sendCode(String email) {
        redis.delete(KEY_PREFIX + email);

        String code = String.format("%06d", RND.nextInt(1_000_000));
        String hash = sha256Base64(code);

        redis.opsForValue().set(
                KEY_PREFIX + email,
                hash,
                Duration.ofMinutes(ttlMinutes)
        );

        sendHtml(email, "[GRADU] 이메일 인증코드",
                """
                <h2>이메일 인증코드</h2>
                <p>아래 코드를 회원가입 화면에 입력하세요.</p>
                <div style="font-size:24px;font-weight:700;letter-spacing:6px;">%s</div>
                <p>유효시간: %d분</p>
                """.formatted(code, ttlMinutes));
    }

    public void verifyCode(String email, String rawCode) {
        String key = KEY_PREFIX + email;
        String storedHash = redis.opsForValue().get(key);

        if (storedHash == null) {
            throw new EmailException(ErrorCode.EMAIL_OTP_EXPIRED);
        }

        if (!isCodeValid(storedHash, rawCode)) {
            throw new EmailException(ErrorCode.EMAIL_OTP_INVALID);
        }

        redis.delete(key);
    }

    private static String sha256Base64(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new EmailException(ErrorCode.EMAIL_HASH_ERROR);
        }
    }

    private static boolean isCodeValid(String storedBase64, String rawCode) {
        byte[] storedHash = Base64.getUrlDecoder().decode(storedBase64);
        byte[] inputHash = sha256(rawCode);
        return MessageDigest.isEqual(storedHash, inputHash);
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new EmailException(ErrorCode.EMAIL_HASH_ERROR);
        }
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new EmailException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
