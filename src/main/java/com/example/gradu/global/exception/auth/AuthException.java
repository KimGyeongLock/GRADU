package com.example.gradu.global.exception.auth;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class AuthException extends BaseException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}