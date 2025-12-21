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
import com.example.gradu.global.exception.auth.AuthException;
import com.example.gradu.global.exception.student.StudentException;
import com.example.gradu.global.security.jwt.JwtTokenProvider;
import com.example.gradu.global.security.jwt.RefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenStore refreshTokenStore;
    @Mock CurriculumService curriculumService;
    @Mock EmailVerificationService emailVerificationService;
    @Mock SummaryService summaryService;
    @Mock CourseService courseService;

    @InjectMocks StudentService studentService;

    private Student student(Long id, String encodedPw) {
        Student s = Student.builder()
                .email("a@handong.ac.kr")
                .password(encodedPw)
                .emailHash("hash")
                .build();

        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }



    // ---------------- register ----------------

    @Test
    void register_alreadyExists_throwsStudentException() {
        // given
        String email = "a@handong.ac.kr";
        String code = "123456";
        String pw = "pw";
        when(studentRepository.existsByEmailHash(Sha256.hash(email))).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> studentService.register(email, pw, code))
                .isInstanceOf(StudentException.class);

        verify(studentRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService, curriculumService);
    }

    @Test
    void register_success_savesStudent_initsCurriculum_andConsumesCode() {
        // given
        String email = "a@handong.ac.kr";
        String code = "123456";
        String rawPw = "pw";
        String encodedPw = "ENC";

        when(studentRepository.existsByEmailHash(Sha256.hash(email))).thenReturn(false);
        when(passwordEncoder.encode(rawPw)).thenReturn(encodedPw);

        // save 시점에 student.getId()가 필요한데, save 호출될 때 ID가 세팅된 것처럼 stub
        // 실제 JPA면 save 후 id가 생기지만 유닛테스트에서는 흉내내야 함
        doAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        }).when(studentRepository).save(any(Student.class));

        // when
        studentService.register(email, rawPw, code);

        // then
        verify(emailVerificationService).verifyCodeOnly(email, code);
        verify(studentRepository).save(any(Student.class));
        verify(curriculumService).initializeForStudent(1L);
        verify(emailVerificationService).consumeCode(email);
    }

    // ---------------- login ----------------

    @Test
    void login_studentNotFound_throwsStudentException() {
        // given
        String email = "a@handong.ac.kr";
        when(studentRepository.findByEmailHash(Sha256.hash(email))).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studentService.login(email, "pw"))
                .isInstanceOf(StudentException.class);

        verifyNoInteractions(jwtTokenProvider, refreshTokenStore);
    }

    @Test
    void login_passwordMismatch_throwsAuthException() {
        // given
        String email = "a@handong.ac.kr";
        Student s = student(1L, "ENC");
        when(studentRepository.findByEmailHash(Sha256.hash(email))).thenReturn(Optional.of(s));
        when(passwordEncoder.matches("wrong", "ENC")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> studentService.login(email, "wrong"))
                .isInstanceOf(AuthException.class);

        verifyNoInteractions(jwtTokenProvider, refreshTokenStore);
    }

    @Test
    void login_success_generatesTokens_andSavesRefreshToken() {
        // given
        String email = "a@handong.ac.kr";
        Student s = student(7L, "ENC");
        when(studentRepository.findByEmailHash(Sha256.hash(email))).thenReturn(Optional.of(s));
        when(passwordEncoder.matches("pw", "ENC")).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(7L)).thenReturn("ACCESS");
        when(jwtTokenProvider.generateRefreshToken(7L)).thenReturn("REFRESH");

        // when
        LoginResponseDto res = studentService.login(email, "pw");

        // then
        assertThat(res.getAccessToken()).isEqualTo("ACCESS");
        assertThat(res.getRefreshToken()).isEqualTo("REFRESH");
        verify(refreshTokenStore).save("REFRESH", 7L);
    }

    // ---------------- reissue ----------------

    @Test
    void reissue_invalidRefreshToken_throwsAuthException() {
        // given
        when(jwtTokenProvider.isTokenValid("R")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> studentService.reissue("R"))
                .isInstanceOf(AuthException.class);

        verifyNoInteractions(refreshTokenStore, studentRepository);
    }

    @Test
    void reissue_refreshNotStored_throwsAuthException() {
        // given
        when(jwtTokenProvider.isTokenValid("R")).thenReturn(true);
        when(jwtTokenProvider.getStudentIdFromToken("R")).thenReturn(1L);
        when(refreshTokenStore.validate("R")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> studentService.reissue("R"))
                .isInstanceOf(AuthException.class);

        verify(studentRepository, never()).findById(anyLong());
    }

    @Test
    void reissue_studentNotFound_throwsStudentException() {
        // given
        when(jwtTokenProvider.isTokenValid("R")).thenReturn(true);
        when(jwtTokenProvider.getStudentIdFromToken("R")).thenReturn(1L);
        when(refreshTokenStore.validate("R")).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studentService.reissue("R"))
                .isInstanceOf(StudentException.class);
    }

    @Test
    void reissue_success_returnsNewAccessToken() {
        // given
        when(jwtTokenProvider.isTokenValid("R")).thenReturn(true);
        when(jwtTokenProvider.getStudentIdFromToken("R")).thenReturn(1L);
        when(refreshTokenStore.validate("R")).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "ENC")));
        when(jwtTokenProvider.generateAccessToken(1L)).thenReturn("NEW_ACCESS");

        // when
        String token = studentService.reissue("R");

        // then
        assertThat(token).isEqualTo("NEW_ACCESS");
    }

    // ---------------- logout ----------------

    @Test
    void logout_invalidToken_throwsAuthException() {
        // given
        when(refreshTokenStore.validate("R")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> studentService.logout("R"))
                .isInstanceOf(AuthException.class);

        verify(refreshTokenStore, never()).remove(anyString());
    }

    @Test
    void logout_success_removesRefreshToken() {
        // given
        when(refreshTokenStore.validate("R")).thenReturn(true);

        // when
        studentService.logout("R");

        // then
        verify(refreshTokenStore).remove("R");
    }

    // ---------------- withdraw ----------------

    @Test
    void withdraw_refreshProvidedButInvalid_throwsAuthException() {
        // given
        when(refreshTokenStore.validate("R")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> studentService.withdraw(1L, "R"))
                .isInstanceOf(AuthException.class);

        verifyNoInteractions(studentRepository, curriculumService, summaryService, courseService);
    }

    @Test
    void withdraw_refreshProvided_valid_removesEverything_andDeletesStudent() {
        // given
        when(refreshTokenStore.validate("R")).thenReturn(true);
        Student s = student(1L, "ENC");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        // when
        studentService.withdraw(1L, "R");

        // then
        verify(refreshTokenStore).remove("R");
        verify(curriculumService).removeForStudent(1L);
        verify(summaryService).removeForStudent(1L);
        verify(courseService).removeForStudent(1L);
        verify(studentRepository).delete(s);
    }

    @Test
    void withdraw_refreshNullOrBlank_skipsTokenValidation_andDeletesStudent() {
        // given
        Student s = student(1L, "ENC");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        // when
        studentService.withdraw(1L, "  ");

        // then
        verifyNoInteractions(refreshTokenStore);
        verify(curriculumService).removeForStudent(1L);
        verify(summaryService).removeForStudent(1L);
        verify(courseService).removeForStudent(1L);
        verify(studentRepository).delete(s);
    }

    // ---------------- resetPassword ----------------

    @Test
    void resetPassword_studentNotFound_throwsStudentException() {
        // given
        PasswordResetRequestDto req = mock(PasswordResetRequestDto.class);
        when(req.email()).thenReturn("a@handong.ac.kr");
        when(req.code()).thenReturn("123456");

        when(studentRepository.findByEmailHash(Sha256.hash("a@handong.ac.kr"))).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studentService.resetPassword(req))
                .isInstanceOf(StudentException.class);

        verify(emailVerificationService).verifyCodeOnly("a@handong.ac.kr", "123456");
        verify(emailVerificationService, never()).consumeCode(anyString());
    }

    @Test
    void resetPassword_samePassword_throwsStudentException() {
        // given
        PasswordResetRequestDto req = mock(PasswordResetRequestDto.class);
        when(req.email()).thenReturn("a@handong.ac.kr");
        when(req.code()).thenReturn("123456");
        when(req.newPassword()).thenReturn("pw");

        Student s = student(1L, "ENC");
        when(studentRepository.findByEmailHash(Sha256.hash("a@handong.ac.kr"))).thenReturn(Optional.of(s));
        when(passwordEncoder.matches("pw", "ENC")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> studentService.resetPassword(req))
                .isInstanceOf(StudentException.class);

        verify(emailVerificationService, never()).consumeCode(anyString());
        assertThat(s.getPassword()).isEqualTo("ENC");
    }

    @Test
    void resetPassword_success_changesPassword_andConsumesCode() {
        // given
        PasswordResetRequestDto req = mock(PasswordResetRequestDto.class);
        when(req.email()).thenReturn("a@handong.ac.kr");
        when(req.code()).thenReturn("123456");
        when(req.newPassword()).thenReturn("new");

        Student s = student(1L, "ENC");
        when(studentRepository.findByEmailHash(Sha256.hash("a@handong.ac.kr"))).thenReturn(Optional.of(s));
        when(passwordEncoder.matches("new", "ENC")).thenReturn(false);
        when(passwordEncoder.encode("new")).thenReturn("NEW_ENC");

        // when
        studentService.resetPassword(req);

        // then
        assertThat(s.getPassword()).isEqualTo("NEW_ENC");
        verify(emailVerificationService).consumeCode("a@handong.ac.kr");
    }
}
