package com.example.gradu.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesGcmUtil aes;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aes.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return aes.decrypt(dbData);
    }
}
