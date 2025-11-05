package com.example.gradu.domain.email.service;

import com.example.gradu.domain.email.entity.EmailVerificationToken;
import com.example.gradu.domain.email.repository.EmailVerificationTokenRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.email.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repo;
    private final JavaMailSender mailSender;

    @Value("${app.email.otp-ttl-minutes}")
    private long ttlMinutes;

    private static final SecureRandom RND = new SecureRandom();

    @Transactional
    public void sendCode(String email) {
        // 최근 코드 삭제(선택)
        repo.deleteByEmail(email);

        String code = String.format("%06d", RND.nextInt(1_000_000));
        String hash = sha256Base64(code);

        EmailVerificationToken otp = EmailVerificationToken.builder()
                .email(email)
                .codeHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .used(false)
                .build();
        repo.save(otp);

        sendHtml(email, "[GRADU] 이메일 인증코드",
                """
                <h2>이메일 인증코드</h2>
                <p>아래 코드를 회원가입 화면에 입력하세요.</p>
                <div style="font-size:24px;font-weight:700;letter-spacing:6px;">%s</div>
                <p>유효시간: %d분</p>
                """.formatted(code, ttlMinutes));
    }

    @Transactional
    public boolean verifyCode(String email, String rawCode) {
        Optional<EmailVerificationToken> tokenOpt = repo.findTopByEmailOrderByIdDesc(email)
                .filter(o -> !o.isExpired() && !o.isUsed())
                .filter(token -> isCodeValid(token, rawCode));

        tokenOpt.ifPresent(token -> {
            token.markUsed();
            repo.save(token);
        });

        return tokenOpt.isPresent();
    }

    private boolean isCodeValid(EmailVerificationToken token, String rawCode) {
        byte[] storedHash;
        try {
            storedHash = Base64.getUrlDecoder().decode(token.getCodeHash());
        } catch (IllegalArgumentException ex) {
            // DB에 저장된 해시가 유효하지 않은 Base64 형식이면 검증 실패
            log.warn("Failed to decode Base64 code hash for email:", ex);
            return false;
        }

        byte[] inputDigest = sha256Bytes(rawCode);
        // 타이밍 공격에 안전한 비교
        return MessageDigest.isEqual(storedHash, inputDigest);
    }

    private static byte[] sha256Bytes(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new EmailException(ErrorCode.EMAIL_HASH_ERROR);
        }
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
