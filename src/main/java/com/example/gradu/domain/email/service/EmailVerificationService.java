package com.example.gradu.domain.email.service;

import com.example.gradu.domain.email.entity.EmailVerificationToken;
import com.example.gradu.domain.email.repository.EmailVerificationTokenRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.email.EmailException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repo;
    private final JavaMailSender mailSender;

    @Value("${app.email.otp-ttl-minutes}")
    private long ttlMinutes;

    private static final Random RND = new Random();

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
                .filter(o -> {
                    // stored hash는 Base64 URL-safe로 저장되어 있다고 가정
                    byte[] stored;
                    try {
                        stored = Base64.getUrlDecoder().decode(o.getCodeHash());
                    } catch (IllegalArgumentException ex) {
                        // DB에 이상한 값이 있으면 비교 실패 처리
                        return false;
                    }

                    byte[] inputDigest = sha256Bytes(rawCode);
                    // MessageDigest.isEqual은 타이밍 공격에 강한 비교 함수입니다.
                    return MessageDigest.isEqual(stored, inputDigest);
                });

        tokenOpt.ifPresent(token -> {
            token.markUsed();
            repo.save(token);
        });

        return tokenOpt.isPresent();
    }

    private static byte[] sha256Bytes(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // 프로젝트 예외로 래핑 (AuthException 예시)
            throw new EmailException(ErrorCode.EMAIL_HASH_ERROR);
        }
    }


    private static String sha256Base64(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
        } catch (Exception e) {
            throw new EmailException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
