package com.example.gradu.global.security.jwt;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {
    //TODO: redis 저장
    public void save(String studentId, String refreshToken) {

    }

    public boolean validate(String studentId, String refreshToken) {
        return true;
    }

    public void remove(String studentId) {
    }
}
