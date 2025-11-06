package com.example.gradu.domain.student.controller;

import com.example.gradu.domain.student.dto.AccessTokenResponseDto;
import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.example.gradu.domain.student.service.StudentService;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import com.example.gradu.global.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String REFRESH_TOKEN = "refreshToken";
    private final StudentService studentService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody StudentAuthRequestDto request) {
        studentService.register(request.getStudentId(), request.getPassword(), request.getName(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDto> login(@Valid @RequestBody StudentAuthRequestDto request, HttpServletResponse response) {
        LoginResponseDto tokens = studentService.login(request.getStudentId(), request.getPassword());

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshExpiration()))
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(new AccessTokenResponseDto(tokens.getAccessToken()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<AccessTokenResponseDto> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = studentService.reissue(refreshToken);
        return ResponseEntity.ok(new AccessTokenResponseDto(newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            // 쿠키가 없으면 이미 로그아웃된 상태로 간주: 204로 조용히 성공 처리해도 됨
            return noContentAndDeleteCookie(response);
        }

        // 서버측 저장소(예: Redis)에서 refresh 폐기
        studentService.logout(null, refreshToken); // access 불필요

        // 동일 속성으로 삭제 쿠키 내려주기(secure/samesite/path 일치)
        return noContentAndDeleteCookie(response);
    }
    private ResponseEntity<Void> noContentAndDeleteCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)          // 삭제
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

}
