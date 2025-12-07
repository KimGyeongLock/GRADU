package com.example.gradu.global.migration;

import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.crypto.AesGcmUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)  // 필요하면 순서 조정
public class EmailEncryptMigrationRunner implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final AesGcmUtil aes;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 마이그레이션 한 뒤엔 더 이상 돌리고 싶지 않으면
        // 간단히 조건 걸어도 됨 (예: "이미 암호화된 것 같으면 return")

        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) return;

        for (Student s : students) {
            String email = s.getEmail();
            if (email == null || email.isBlank()) continue;

            // 이미 암호화된 것인지 간단 체크 (Base64 pattern 정도로):
            if (looksLikeEncrypted(email)) {
                continue;
            }

            String encrypted = aes.encrypt(email);
            s.changeEmail(encrypted); // 혹은 setEmail, 또는 직접 필드 수정
        }
        // @Transactional 이라 여기서 flush 되면서 DB에 암호문이 저장됨
    }

    private boolean looksLikeEncrypted(String value) {
        // 매우 러프하게 "이미 Base64 로 인코딩된 것 같다" 정도 체크
        // 필요 없으면 그냥 false 리턴해서 무조건 암호화해도 됨(단, 중복 암호화만 조심)
        return value.length() > 40 && value.matches("^[0-9A-Za-z+/]+=*$");
    }
}
