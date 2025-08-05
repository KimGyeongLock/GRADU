package com.example.gradu.global.exception.student;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class StudentException extends BaseException {
    public StudentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
