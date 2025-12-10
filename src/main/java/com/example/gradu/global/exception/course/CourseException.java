package com.example.gradu.global.exception.course;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
public class CourseException extends BaseException {

    private final List<String> duplicates;

    public CourseException(ErrorCode errorCode) {
        super(errorCode);
        this.duplicates = null;
    }

    public CourseException(ErrorCode errorCode, List<String> duplicates) {
        super(errorCode);
        this.duplicates = duplicates;
    }
}
