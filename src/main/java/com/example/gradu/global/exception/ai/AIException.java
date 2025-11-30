package com.example.gradu.global.exception.ai;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class AIException extends BaseException {
    public AIException(ErrorCode errorCode) {
        super(errorCode);
    }
}
