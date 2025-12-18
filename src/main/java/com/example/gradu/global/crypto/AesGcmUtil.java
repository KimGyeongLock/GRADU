package com.example.gradu.global.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmUtil {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;   // bits
    private static final int IV_LENGTH = 12;         // bytes

    @Value("${app.crypto.email-key}")
    private String keyString;

    private SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        byte[] keyBytes = hexToBytes(keyString.trim());
        this.keySpec = new SecretKeySpec(keyBytes, ALGO);
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return plain;

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // [IV + CipherText] 를 Base64 로 인코딩
            byte[] res = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, res, 0, iv.length);
            System.arraycopy(enc, 0, res, iv.length, enc.length);

            return Base64.getEncoder().encodeToString(res);
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt 실패", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return cipherText;

        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            byte[] enc = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, enc, 0, enc.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            byte[] plain = cipher.doFinal(enc);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt 실패", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] res = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            res[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return res;
    }
}
