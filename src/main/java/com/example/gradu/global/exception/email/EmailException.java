package com.example.gradu.global.exception.email;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class EmailException extends BaseException {
    public EmailException(ErrorCode errorCode) {
        super(errorCode);
    }
}
