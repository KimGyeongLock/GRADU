package com.example.gradu.global.exception.curriculum;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class CurriculumException extends BaseException {
    public CurriculumException(ErrorCode errorCode) {
        super(errorCode);
    }
}
