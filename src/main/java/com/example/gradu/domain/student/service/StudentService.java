package com.example.gradu.domain.student.service;

import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.service.CurriculumService;
import com.example.gradu.domain.email.service.EmailVerificationService;
import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.dto.PasswordResetRequestDto;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.service.SummaryService;
import com.example.gradu.global.crypto.Sha256;
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
    private final CurriculumService curriculumService;
    private final EmailVerificationService emailVerificationService;
    private final SummaryService summaryService;
    private final CourseService courseService;

    @Transactional
    public void register(String email, String password, String code) {

        String emailHash = Sha256.hash(email);

        if (studentRepository.existsByEmailHash(emailHash)) {
            throw new StudentException(ErrorCode.STUDENT_ALREADY_EXISTS);
        }

        emailVerificationService.verifyCodeOnly(email, code);

        String encodedPassword = passwordEncoder.encode(password);

        Student student = Student.builder()
                .password(encodedPassword)
                .email(email)
                .emailHash(emailHash)
                .build();

        studentRepository.save(student);

        curriculumService.initializeForStudent(student.getId());

        emailVerificationService.consumeCode(email);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(String email, String rawPassword) {
        String emailHash = Sha256.hash(email);
        Student student = studentRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, student.getPassword()))
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);

        Long id = student.getId();
        String accessToken = jwtTokenProvider.generateAccessToken(id);
        String refreshToken = jwtTokenProvider.generateRefreshToken(id);

        refreshTokenStore.save(refreshToken, id);
        return new LoginResponseDto(accessToken, refreshToken);
    }

    public String reissue(String refreshToken){
        if (!jwtTokenProvider.isTokenValid(refreshToken)){
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long studentId = jwtTokenProvider.getStudentIdFromToken(refreshToken);

        if (!refreshTokenStore.validate(refreshToken)){
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }
        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        return jwtTokenProvider.generateAccessToken(studentId);
    }

    public void logout(String refreshToken) {
        if (!refreshTokenStore.validate(refreshToken)) {
            throw new AuthException(ErrorCode.TOKEN_INVALID);
        }
        refreshTokenStore.remove(refreshToken);
    }

    @Transactional
    public void withdraw(Long studentId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            if (!refreshTokenStore.validate(refreshToken)) {
                throw new AuthException(ErrorCode.TOKEN_INVALID);
            }
            refreshTokenStore.remove(refreshToken);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        curriculumService.removeForStudent(studentId);
        summaryService.removeForStudent(studentId);
        courseService.removeForStudent(studentId);

        studentRepository.delete(student);
    }

    @Transactional
    public void resetPassword(PasswordResetRequestDto req) {
        emailVerificationService.verifyCodeOnly(req.email(), req.code());

        Student student = studentRepository.findByEmailHash(Sha256.hash(req.email()))
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        if (passwordEncoder.matches(req.newPassword(), student.getPassword())) {
            throw new StudentException(ErrorCode.SAME_PASSWORD_NOT_ALLOWED);
        }

        student.changePassword(passwordEncoder.encode(req.newPassword()));

        emailVerificationService.consumeCode(req.email());
    }
}
