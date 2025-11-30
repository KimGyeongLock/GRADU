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
    PASSWORD_MISMATCH("A004", HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),


    CURRICULUM_NOT_FOUND("C001", HttpStatus.NOT_FOUND, "구분을 찾을 수 없습니다."),
    COURSE_NOT_FOUND("C002", HttpStatus.NOT_FOUND, "과목을 찾을 수 없습니다."),

    SUMMARY_JSON_PROCESSING_ERROR("E001", HttpStatus.BAD_REQUEST, "JSON 직렬화 실패"),

    EMAIL_NOT_VERIFIED("EM001", HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_SEND_FAILED("EM002", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),
    EMAIL_HASH_ERROR("EM003", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 코드 해시 생성에 실패했습니다."),

    AI_IMAGE_CONVERSION_FAILED("AI001", HttpStatus.INTERNAL_SERVER_ERROR, "이미지 변환에 실패했습니다."),
    AI_RESPONSE_PARSING_FAILED("AI002", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 파싱하는 데 실패했습니다.");


    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}