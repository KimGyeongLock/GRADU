package com.example.gradu.global.security.jwt;

import com.example.gradu.global.exception.auth.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    JwtProperties props;
    JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("01234567890123456789012345678901");
        props.setAccessExpiration(60_000);   // 1분
        props.setRefreshExpiration(120_000); // 2분

        provider = new JwtTokenProvider(props);
        provider.init();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // given: access 만료 시간을 1ms로 주고 토큰 생성
        props.setAccessExpiration(1);
        provider = new JwtTokenProvider(props);
        provider.init();

        // when
        String token = provider.generateAccessToken(1L);

        // then
        assertThat(provider.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_wrongSignature_returnsFalse() {
        // given
        String token = provider.generateAccessToken(1L);

        JwtProperties other = new JwtProperties();
        other.setSecret("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        other.setAccessExpiration(60_000);
        other.setRefreshExpiration(60_000);

        // when
        JwtTokenProvider otherProvider = new JwtTokenProvider(other);
        otherProvider.init();

        // then
        assertThat(otherProvider.isTokenValid(token)).isFalse();
    }

    @Test
    void extractStudentIdIgnoringExpiration_expiredToken_returnsSubject() {
        // given
        props.setAccessExpiration(1);
        provider = new JwtTokenProvider(props);
        provider.init();

        // when
        String token = provider.generateAccessToken(777L);

        // then: 만료여도 subject는 뽑혀야 함
        assertThat(provider.extractStudentIdIgnoringExpiration(token)).isEqualTo("777");
    }

    @Test
    void extractStudentIdIgnoringExpiration_malformedToken_throwsAuthException() {
        // 파싱 실패
        assertThatThrownBy(() -> provider.extractStudentIdIgnoringExpiration("not-a-jwt"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = provider.generateAccessToken(1L);
        assertThat(provider.isTokenValid(token)).isTrue();
    }

    @Test
    void getStudentIdFromToken_validToken_returnsStudentId() {
        String token = provider.generateAccessToken(123L);
        assertThat(provider.getStudentIdFromToken(token)).isEqualTo(123L);
    }

    @Test
    void extractStudentIdIgnoringExpiration_validToken_returnsSubject() {
        String token = provider.generateAccessToken(777L);
        assertThat(provider.extractStudentIdIgnoringExpiration(token)).isEqualTo("777");
    }

    @Test
    void isTokenValid_nullToken_returnsFalse() {
        assertThat(provider.isTokenValid(null)).isFalse();
    }

    @Test
    void isTokenValid_jweLikeToken_returnsFalse() {
        String jweLike = "a.b.c.d.e";
        assertThat(provider.isTokenValid(jweLike)).isFalse();
    }

    @Test
    void isTokenValid_algNoneToken_returnsFalse() {
        // {"alg":"none"} base64url = eyJhbGciOiJub25lIn0
        // {"sub":"1"} base64url = eyJzdWIiOiIxIn0
        String algNone = "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0.";
        assertThat(provider.isTokenValid(algNone)).isFalse();
    }

}
