package com.example.gradu.global.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Map<String, String> errors;   // Validation errors
    private final List<String> duplicates;      // Bulk 중복 과목명 용
}
