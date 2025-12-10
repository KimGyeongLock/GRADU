package com.example.gradu.global.exception;

import com.example.gradu.global.exception.course.CourseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 🔥 CourseException → BaseException 보다 먼저 선언해야 함 */
    @ExceptionHandler(CourseException.class)
    public ResponseEntity<ErrorResponse> handleCourseException(CourseException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse body = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(ex.getMessage())      // BulkInsert 시 커스텀 메시지 사용
                .errors(null)
                .duplicates(ex.getDuplicates()) // 🔥 프론트에서 사용하는 필드
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(body);
    }

    /** 🔥 BaseException (CourseException 아닌 모든 BaseException) */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();

        ErrorResponse body = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(null)
                .duplicates(null)
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(body);
    }

    /** Validation 에러 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse body = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT.getCode())
                .message(ErrorCode.INVALID_INPUT.getMessage())
                .errors(errors)
                .duplicates(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }
}
