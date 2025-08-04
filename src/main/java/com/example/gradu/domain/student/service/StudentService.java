package com.example.gradu.domain.student.service;

import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.auth.AuthException;
import com.example.gradu.global.exception.student.StudentException;
import com.example.gradu.global.security.jwt.JwtTokenProvider;
import com.example.gradu.global.security.jwt.RefreshTokenStore;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public void register(String studentId, String password) {
        if (studentRepository.findByStudentId(studentId).isPresent())
            throw new StudentException(ErrorCode.STUDENT_ALREADY_EXISTS);

        String encodedPassword = passwordEncoder.encode(password);
        String email = studentId + "@handong.ac.kr";
        Student student = Student.builder()
                .studentId(studentId)
                .password(encodedPassword)
                .email(email)
                .build();

        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(String studentId, String rawPassword) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, student.getPassword()))
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);

        String accessToken = jwtTokenProvider.generateAccessToken(studentId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(studentId);

        refreshTokenStore.save(studentId, refreshToken);
        return new LoginResponseDto(accessToken, refreshToken);
    }

    public String reissue(String refreshToken){
        if (!jwtTokenProvider.isTokenValid(refreshToken)){
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String studentId = jwtTokenProvider.getStudentIdFromToken(refreshToken);

        if (!refreshTokenStore.validate(studentId, refreshToken)){
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }
        return jwtTokenProvider.generateAccessToken(studentId);
    }

    public void logout(String accessToken, String refreshToken) {
        String studentId = jwtTokenProvider.extractStudentIdIgnoringExpiration(accessToken);

        if (!refreshTokenStore.validate(studentId, refreshToken)) {
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }

        refreshTokenStore.remove(studentId);
    }

}
