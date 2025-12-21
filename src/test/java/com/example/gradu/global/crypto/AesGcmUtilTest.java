package com.example.gradu.global.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AesGcmUtilTest {

    @Autowired
    AesGcmUtil aes;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.crypto.email-key", () -> randomHex(32)); // 32 bytes = 256-bit
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new java.security.SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginal() {
        // given
        String plain = "handong@handong.ac.kr";

        // when
        String enc = aes.encrypt(plain);
        String dec = aes.decrypt(enc);

        // then
        assertThat(enc).isNotBlank();
        assertThat(dec).isEqualTo(plain);
    }

    @Test
    void encrypt_samePlaintext_shouldProduceDifferentCiphertextBecauseRandomIv() {
        // given
        String plan = "same-text";

        // when
        String c1 = aes.encrypt(plan);
        String c2 = aes.encrypt(plan);

        // then
        assertThat(c1).isNotEqualTo(c2);
        assertThat(aes.decrypt(c1)).isEqualTo(plan);
        assertThat(aes.decrypt(c2)).isEqualTo(plan);
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
        // given
        String enc = aes.encrypt("hello");

        // when
        String tampered = enc.substring(0, enc.length() - 2) + "aa";

        // then
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
