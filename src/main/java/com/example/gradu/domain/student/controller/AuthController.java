package com.example.gradu.domain.student.controller;

import com.example.gradu.domain.student.dto.AccessTokenResponseDto;
import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.dto.PasswordResetRequestDto;
import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.example.gradu.domain.student.service.StudentService;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import com.example.gradu.global.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String REFRESH_TOKEN = "refreshToken";
    private final StudentService studentService;
    private final JwtProperties jwtProperties;

    private final String frontendDomain;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            StudentService studentService,
            JwtProperties jwtProperties,
            @Value("${app.frontend-domain}") String frontendDomain,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.cookie.same-site}") String cookieSameSite
    ) {
        this.studentService = studentService;
        this.jwtProperties = jwtProperties;
        this.frontendDomain = frontendDomain;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody StudentAuthRequestDto request) {
        studentService.register(request.getEmail(), request.getPassword(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDto> login(@Valid @RequestBody StudentAuthRequestDto request, HttpServletResponse response) {
        LoginResponseDto tokens = studentService.login(request.getEmail(), request.getPassword());

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, tokens.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshExpiration()))
                .sameSite(cookieSameSite)
                .domain(frontendDomain)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(new AccessTokenResponseDto(tokens.getAccessToken()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<AccessTokenResponseDto> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith(JwtAuthenticationFilter.TOKEN_PREFIX)) {
                refreshToken = auth.substring(JwtAuthenticationFilter.TOKEN_PREFIX.length());
            }
        }
        if (refreshToken == null || refreshToken.isBlank()) {
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
            return noContentAndDeleteCookie(response);
        }

        studentService.logout(refreshToken);

        return noContentAndDeleteCookie(response);
    }
    private ResponseEntity<Void> noContentAndDeleteCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .domain(frontendDomain)
                .path("/")
                .maxAge(0)
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

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long studentId = (Long) authentication.getPrincipal();
        String refreshToken = extractRefreshTokenFromCookie(request);

        studentService.withdraw(studentId, refreshToken);

        return noContentAndDeleteCookie(response);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetRequestDto req) {
        studentService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
