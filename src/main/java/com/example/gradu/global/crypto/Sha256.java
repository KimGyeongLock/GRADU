package com.example.gradu.global.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Sha256 {

    public static String hash(String input) {
        java.util.Objects.requireNonNull(input, "Input for hashing cannot be null.");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
