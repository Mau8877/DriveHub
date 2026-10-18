package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }
}
