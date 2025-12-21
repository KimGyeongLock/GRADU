package com.example.gradu.global.crypto;

import com.example.gradu.global.exception.crypto.CryptoException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.example.gradu.global.exception.ErrorCode.SHA_256_HASH_FAILED;

public class Sha256 {
    private Sha256() {}

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

        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(SHA_256_HASH_FAILED);
        }
    }
}
