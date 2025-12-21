package com.example.gradu.global.exception.crypto;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class CryptoException extends BaseException {
    public CryptoException(ErrorCode errorCode) {
        super(errorCode);
    }
}