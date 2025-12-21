package com.example.gradu.global.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenStoreTest {

    RedisTemplate<String, String> redisTemplate;
    ValueOperations<String, String> valueOps;
    JwtProperties props;
    RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        props = new JwtProperties();
        props.setRefreshExpiration(120_000);

        store = new RefreshTokenStore(redisTemplate, props);
    }

    @Test
    void save_setsValueWithTtl() {
        store.save("rt", 10L);

        verify(valueOps).set(eq("rt"), eq("10"), eq(120_000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void validate_returnsTrue_whenExists() {
        when(valueOps.get("rt")).thenReturn("10");

        assertThat(store.validate("rt")).isTrue();
    }

    @Test
    void validate_returnsFalse_whenMissing() {
        when(valueOps.get("rt")).thenReturn(null);

        assertThat(store.validate("rt")).isFalse();
    }

    @Test
    void remove_deletesKey() {
        store.remove("rt");
        verify(redisTemplate).delete("rt");
    }
}
