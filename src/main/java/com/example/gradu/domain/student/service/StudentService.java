package com.example.gradu.domain.student.service;

import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.security.jwt.JwtTokenProvider;
import com.example.gradu.global.security.jwt.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public void register(String studentId, String password) {
        if (studentRepository.findByStudentId(studentId).isPresent())
            throw new IllegalArgumentException("이미 존재하는 학번입니다.");

        String encodedPassword = passwordEncoder.encode(password);
        String email = studentId + "@handong.ac.kr";
        Student student = Student.of(studentId, encodedPassword, email);
        studentRepository.save(student);
    }

    public LoginResponseDto login(String studentId, String rawPassword) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학번이 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPassword, student.getPassword()))
            throw new IllegalArgumentException("비밀번호가 틀립니다.");

        String accessToken = jwtTokenProvider.generateAccessToken(studentId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(studentId);

        refreshTokenStore.save(studentId, refreshToken);
        return new LoginResponseDto(accessToken, refreshToken);
    }

    public String reissue(String refreshToken){
        if (!jwtTokenProvider.isTokenValid(refreshToken)){
            throw new RuntimeException("Refresh Token이 유효하지 않음");
        }

        String studentId = jwtTokenProvider.getStudentIdFromToken(refreshToken);

        if (!refreshTokenStore.validate(studentId, refreshToken)){
            throw new RuntimeException("Refresh Token 불일치");
        }
        return jwtTokenProvider.generateAccessToken(studentId);
    }

    public void logout(String accessToken, String refreshToken) {
        String studentId = jwtTokenProvider.getStudentIdFromToken(accessToken);

        if (!refreshTokenStore.validate(studentId, refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token");
        }

        refreshTokenStore.remove(studentId);
    }

}
