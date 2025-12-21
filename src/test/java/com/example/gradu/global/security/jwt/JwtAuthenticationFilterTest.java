package com.example.gradu.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.example.gradu.global.security.jwt.JwtAuthenticationFilter.TOKEN_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication_andCallsChain() throws Exception {
        // given
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/courses"); // 화이트리스트 X
        req.addHeader("Authorization", TOKEN_PREFIX + "valid-token");

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(provider.isTokenValid("valid-token")).thenReturn(true);
        when(provider.getStudentIdFromToken("valid-token")).thenReturn(1L);

        // when
        filter.doFilter(req, res, chain);

        // then: 정상 작동 (컨텍스트 유저 정보 저장) + 다음 필터로 패스
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_invalidToken_doesNotSetAuthentication_butCallsChain() throws Exception {
        // given
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/courses");
        req.addHeader("Authorization", TOKEN_PREFIX + "bad-token");

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(provider.isTokenValid("bad-token")).thenReturn(false);

        // when
        filter.doFilter(req, res, chain);

        // then: 잘못된 토큰 + 다음 필터로 패스
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
        verify(provider, never()).getStudentIdFromToken(anyString());
    }

    @Test
    void doFilterInternal_tokenProviderThrowsJwtException_silentlyPasses() throws Exception {
        // given
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/courses");
        req.addHeader("Authorization", TOKEN_PREFIX + "boom");

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(provider.isTokenValid("boom")).thenThrow(new JwtException("invalid"));

        // when
        filter.doFilter(req, res, chain);

        // then: JwtException(만료나 invalid) 시 컨텍스트 저장 X + 다음 필터로 패스
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_justPasses() throws Exception {
        // given
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/courses");

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(req, res, chain);

        // then: header가 없을 경우, 컨텍스트 저장 X + 다음 필터로 패스
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
        verifyNoInteractions(provider);
    }
}
