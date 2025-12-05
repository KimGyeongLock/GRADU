package com.example.gradu.domain.student.service;

import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.service.CurriculumService;
import com.example.gradu.domain.email.service.EmailVerificationService;
import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.dto.PasswordResetRequestDto;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.service.SummaryService;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.auth.AuthException;
import com.example.gradu.global.exception.email.EmailException;
import com.example.gradu.global.exception.student.StudentException;
import com.example.gradu.global.security.jwt.JwtTokenProvider;
import com.example.gradu.global.security.jwt.RefreshTokenStore;
import jakarta.validation.Valid;
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
    private final CurriculumService curriculumService;
    private final EmailVerificationService emailVerificationService;
    private final SummaryService summaryService;
    private final CourseService courseService;

    @Transactional
    public void register(String studentId, String password, String name, String code, String email) {
        if (studentRepository.findByStudentId(studentId).isPresent())
            throw new StudentException(ErrorCode.STUDENT_ALREADY_EXISTS);

        emailVerificationService.verifyCode(email, code);

        String encodedPassword = passwordEncoder.encode(password);

        Student student = Student.builder()
                .studentId(studentId)
                .password(encodedPassword)
                .name(name)
                .email(email)
                .emailVerified(true)
                .build();

        studentRepository.save(student);
        curriculumService.initializeForStudent(studentId);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(String studentId, String rawPassword) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, student.getPassword()))
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);

        String accessToken = jwtTokenProvider.generateAccessToken(studentId, student.getName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(studentId);

        refreshTokenStore.save(refreshToken, studentId);
        return new LoginResponseDto(accessToken, refreshToken);
    }

    public String reissue(String refreshToken){
        if (!jwtTokenProvider.isTokenValid(refreshToken)){
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String studentId = jwtTokenProvider.getStudentIdFromToken(refreshToken);

        if (!refreshTokenStore.validate(refreshToken)){
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));
        return jwtTokenProvider.generateAccessToken(studentId, student.getName());
    }

    public void logout(String accessToken, String refreshToken) {
        String studentId = jwtTokenProvider.extractStudentIdIgnoringExpiration(accessToken);

        if (!refreshTokenStore.validate(refreshToken)) {
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }

        refreshTokenStore.remove(refreshToken);
    }

    @Transactional
    public void withdraw(String studentId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            if (!refreshTokenStore.validate(refreshToken)) {
                throw new AuthException(ErrorCode.TOKEN_INVALID);
            }
            refreshTokenStore.remove(refreshToken);
        }

        // 2) 학생 엔티티 삭제
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        // 필요하다면 커리큘럼 데이터 등도 함께 정리
        curriculumService.removeForStudent(studentId);
        summaryService.removeForStudent(studentId);
        courseService.removeForStudent(studentId);

        studentRepository.delete(student);
    }
  
    public void resetPassword(PasswordResetRequestDto req) {
        // 1) 이메일 OTP 검증 (만료/불일치 예외는 앞에서 만든 EmailVerificationService 사용)
        emailVerificationService.verifyCode(req.getEmail(), req.getCode());

        // 2) 학생 조회
        Student student = studentRepository
                .findByStudentIdAndEmail(req.getStudentId(), req.getEmail())
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        // 3) 기존 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(req.getNewPassword(), student.getPassword())) {
            throw new StudentException(ErrorCode.SAME_PASSWORD_NOT_ALLOWED);
        }

        // 4) 비밀번호 변경 (BCrypt)
        student.changePassword(passwordEncoder.encode(req.getNewPassword()));
    }
}
