package com.example.gradu.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // ==== Student 관련 ====
    STUDENT_ALREADY_EXISTS("S001", "학생이 이미 존재합니다."),
    STUDENT_NOT_FOUND("S002", "학생을 찾을 수 없습니다."),
    INVALID_INPUT("S003", "잘못된 입력입니다."),

    // ==== Auth 관련 ====
    TOKEN_EXPIRED("A001", "토큰이 만료되었습니다."),
    TOKEN_INVALID("A002", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_INVALID("A003", "유효하지 않은 리프레시 토큰입니다."),
    PASSWORD_MISMATCH("A004", "비밀번호가 일치하지 않습니다.");

    //    AUTHENTICATION_FAILED("A001", "인증에 실패했습니다."),
//    UNAUTHORIZED_ACCESS("A002", "권한이 없습니다."),
//    USER_NOT_FOUND("A005", "사용자를 찾을 수 없습니다."),

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
