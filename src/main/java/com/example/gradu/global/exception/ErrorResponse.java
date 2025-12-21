package com.example.gradu.global.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Map<String, String> errors;
    private final List<String> duplicates;

    public static class ErrorResponseBuilder {
        public ErrorResponseBuilder errors(Map<String, String> errors) {
            this.errors = (errors == null) ? Map.of() : Map.copyOf(errors);
            return this;
        }

        public ErrorResponseBuilder duplicates(List<String> duplicates) {
            this.duplicates = (duplicates == null) ? List.of() : List.copyOf(duplicates);
            return this;
        }
    }

    public ErrorResponse(String code,
                         String message,
                         Map<String, String> errors,
                         List<String> duplicates) {
        this.code = code;
        this.message = message;

        // 최종 방어선(중복이어도 OK)
        this.errors = (errors == null) ? Map.of() : Map.copyOf(errors);
        this.duplicates = (duplicates == null) ? List.of() : List.copyOf(duplicates);
    }
}
