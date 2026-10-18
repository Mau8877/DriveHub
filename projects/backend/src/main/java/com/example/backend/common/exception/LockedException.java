package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class LockedException extends BusinessException {

    public LockedException(String errorCode, String message) {
        super(errorCode, HttpStatus.LOCKED, message);
    }
}
