package com.example.gradu.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(String studentId, String refreshToken) {
        log.info("RefreshToken 길이: {} bytes", refreshToken.getBytes().length);
        log.info("StudentId: {}", studentId);

        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(studentId, refreshToken, jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);
    }

    public boolean validate(String studentId, String refreshToken) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String storedRefreshToken = values.get(studentId);
        return refreshToken.equals(storedRefreshToken);
    }

    public void remove(String studentId) {
        redisTemplate.delete(studentId);
    }
}
