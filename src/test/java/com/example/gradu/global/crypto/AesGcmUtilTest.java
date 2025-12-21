package com.example.gradu.global.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "app.crypto.email-key=${TEST_AES_KEY}"
})
@ContextConfiguration(classes = AesGcmUtilTest.Config.class)
class AesGcmUtilTest {

    @Autowired AesGcmUtil aes;

    @TestConfiguration
    @ComponentScan(basePackageClasses = AesGcmUtil.class)
    static class Config { }

    static {
        // 테스트 실행 전에 랜덤 키를 System property로 주입 (64 hex chars = 32 bytes)
        System.setProperty("TEST_AES_KEY", randomHex(32));
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginal() {
        String plain = "handong@handong.ac.kr";
        String enc = aes.encrypt(plain);
        String dec = aes.decrypt(enc);

        assertThat(enc).isNotBlank();
        assertThat(dec).isEqualTo(plain);
    }

    @Test
    void encrypt_samePlaintext_shouldProduceDifferentCiphertextBecauseRandomIv() {
        String plain = "same-text";

        String c1 = aes.encrypt(plain);
        String c2 = aes.encrypt(plain);

        assertThat(c1).isNotEqualTo(c2);
        assertThat(aes.decrypt(c1)).isEqualTo(plain);
        assertThat(aes.decrypt(c2)).isEqualTo(plain);
    }

    @Test
    void encrypt_nullOrBlank_returnsAsIs() {
        assertThat(aes.encrypt(null)).isNull();
        assertThat(aes.encrypt("")).isEmpty();
        assertThat(aes.encrypt("    ")).isEqualTo("    ");
    }

    @Test
    void decrypt_nullOrBlank_returnsAsIs() {
        assertThat(aes.decrypt(null)).isNull();
        assertThat(aes.decrypt("")).isEmpty();
        assertThat(aes.decrypt("    ")).isEqualTo("    ");
    }

    @Test
    void decrypt_tamperedCipher_throwsIllegalStateException() {
        String enc = aes.encrypt("hello");
        String tampered = enc.substring(0, enc.length() - 2) + "aa";

        assertThatThrownBy(() -> aes.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AES decrypt 실패");
    }

    @Test
    void decrypt_notBase64_throwsIllegalStateException() {
        assertThatThrownBy(() -> aes.decrypt("not-base64"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AES decrypt 실패");
    }
}
