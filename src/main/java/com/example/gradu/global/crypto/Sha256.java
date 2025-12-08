package com.example.gradu.global.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Sha256 {

    public static String hash(String input) {
        if (input == null) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 바이트 → 16진수(hex) 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // 자리수 보정
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
