package com.example.gradu.global.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EncryptedStringConverterTest {

    @Mock
    AesGcmUtil aes;

    @InjectMocks
    EncryptedStringConverter converter;

    @Test
    void convertToDatabaseColumn_callsEncrypt() {
        // given
        when(aes.encrypt("plain")).thenReturn("enc");

        // when
        String res = converter.convertToDatabaseColumn("plain");

        // then
        assertThat(res).isEqualTo("enc");
        verify(aes).encrypt("plain");
        verifyNoMoreInteractions(aes);
    }

    @Test
    void convertToEntityAttribute_callsDecrypt() {
        // given
        when(aes.decrypt("enc")).thenReturn("plain");

        // when
        String res = converter.convertToEntityAttribute("enc");

        // then
        assertThat(res).isEqualTo("plain");
        verify(aes).decrypt("enc");
        verifyNoMoreInteractions(aes);
    }
}
