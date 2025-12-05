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

    public void save(String refreshToken, String studentId) {
        redisTemplate.opsForValue()
                .set(refreshToken, studentId, jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);
    }

    public boolean validate(String refreshToken) {
        String storedStudentId = redisTemplate.opsForValue().get(refreshToken);
        return storedStudentId != null;
    }

    public void remove(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }
}
