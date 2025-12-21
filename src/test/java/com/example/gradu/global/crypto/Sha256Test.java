package com.example.gradu.global.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256Test {

    @Test
    void hash_knownValue_hello_matchesExpected() {
        // SHA-256("hello")
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertThat(Sha256.hash("hello")).isEqualTo(expected);
    }

    @Test
    void hash_sameInput_producesSameOutput() {
        assertThat(Sha256.hash("abc")).isEqualTo(Sha256.hash("abc"));
    }

    @Test
    void hash_differentInput_producesDifferentOutput() {
        assertThat(Sha256.hash("abc")).isNotEqualTo(Sha256.hash("def"));
    }

    @Test
    void hash_outputIs64LowerHex() {
        String out = Sha256.hash("test");

        assertThat(out)
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    void hash_null_throwsNpe() {
        assertThatThrownBy(() -> Sha256.hash(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Input for hashing cannot be null.");
    }
}
