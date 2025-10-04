package com.example.gradu.domain.email.service;

import com.example.gradu.domain.email.entity.EmailVerificationToken;
import com.example.gradu.domain.email.repository.EmailVerificationTokenRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repo;
    private final JavaMailSender mailSender;

    @Value("${app.email.otp-ttl-minutes:10}")
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
        return repo.findTopByEmailOrderByIdDesc(email)
                .filter(o -> !o.isExpired() && !o.isUsed())
                .filter(o -> o.getCodeHash().equals(sha256Base64(rawCode)))
                .map(o -> { o.markUsed(); repo.save(o); return true; })
                .orElse(false);
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
            throw new RuntimeException("메일 전송 실패", e);
        }
    }
}
