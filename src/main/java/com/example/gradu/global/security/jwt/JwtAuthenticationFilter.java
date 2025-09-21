package com.example.gradu.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    public static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String u = request.getRequestURI();
        return u.startsWith("/api/v1/auth/") ||
                u.startsWith("/v3/api-docs") ||
                u.startsWith("/swagger-ui") ||
                u.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith(TOKEN_PREFIX)) {
            String token = bearer.substring(TOKEN_PREFIX.length());
            try {
                if (jwtTokenProvider.isTokenValid(token)) {
                    String studentId = jwtTokenProvider.getStudentIdFromToken(token);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                // ★ 만료된 access 토큰: 여기서 막지 말고 조용히 통과
                //    (/api는 401/403이 날 수 있고, 프론트가 /auth/refresh를 호출함)
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
                // 잘못된 토큰: 무시하고 다음 필터로
            }
        }
        filterChain.doFilter(request, response);
    }
}
