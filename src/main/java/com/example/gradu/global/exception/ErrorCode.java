package com.example.gradu.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_INPUT("I001", HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),

    // ==== Student 관련 ====
    STUDENT_ALREADY_EXISTS("S001", HttpStatus.CONFLICT, "학생이 이미 존재합니다."),
    STUDENT_NOT_FOUND("S002", HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."),

    // ==== Auth 관련 ====
    TOKEN_EXPIRED("A001", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID("A002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_INVALID("A003", HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    PASSWORD_MISMATCH("A004", HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");


    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}