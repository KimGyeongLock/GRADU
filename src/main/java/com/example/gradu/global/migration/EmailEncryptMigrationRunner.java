package com.example.gradu.global.migration;

import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.crypto.AesGcmUtil;
import com.example.gradu.global.crypto.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class EmailEncryptMigrationRunner implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final AesGcmUtil aes;

    @Override
    @Transactional
    public void run(String... args) {
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) return;

        for (Student s : students) {
            String stored = s.getEmail();
            if (stored == null || stored.isBlank()) continue;

            // 이미 email_hash 가 세팅된 데이터는 스킵 (재실행 대비)
            if (s.getEmailHash() != null && !s.getEmailHash().isBlank()) {
                continue;
            }

            String plain;      // 실제 평문 이메일
            String encrypted;  // DB 에 저장할 암호문

            if (looksLikeEncrypted(stored)) {
                // 이미 암호화된 이메일 → 복호화해서 평문 얻기
                plain = aes.decrypt(stored);
                encrypted = stored;   // 그대로 재사용
            } else {
                // 아직 평문인 이메일 → 그대로 평문으로 쓰고, 새로 암호화
                plain = stored;
                encrypted = aes.encrypt(plain);
            }

            String hash = Sha256.hash(plain);

            s.changeEmail(encrypted, hash);
        }
        // @Transactional 이라 자동 flush
    }

    private boolean looksLikeEncrypted(String value) {
        // 이메일 평문은 보통 "@"가 들어가고, AES+Base64 암호문은 긴 [A-Za-z0-9+/=] 형태라
        // 대충 이렇게 구분 가능
        if (!value.contains("@") && value.length() > 40 && value.matches("^[0-9A-Za-z+/]+=*$")) {
            return true;
        }
        return false;
    }
}
