package com.example.gradu.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import static com.example.gradu.global.security.SecurityConfig.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    public static final String TOKEN_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCH = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String u = request.getRequestURI();
        return isWhitelisted(u);
    }

    private boolean isWhitelisted(String uri) {
        return Stream.concat(
                PUBLIC_WHITELIST.stream(),
                SWAGGER_WHITELIST.stream()
        ).anyMatch(pattern -> PATH_MATCH.match(pattern, uri));
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
                    Long studentId = jwtTokenProvider.getStudentIdFromToken(token);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (io.jsonwebtoken.JwtException
                     | IllegalArgumentException ex) {
                // 만료/위조/잘못된 토큰 모두 조용히 통과
            }
        }
        filterChain.doFilter(request, response);
    }
}
