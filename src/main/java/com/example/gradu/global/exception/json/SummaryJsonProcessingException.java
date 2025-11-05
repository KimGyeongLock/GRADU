package com.example.gradu.global.exception.json;

import com.example.gradu.global.exception.BaseException;
import com.example.gradu.global.exception.ErrorCode;

public class SummaryJsonProcessingException extends BaseException {
    public SummaryJsonProcessingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
