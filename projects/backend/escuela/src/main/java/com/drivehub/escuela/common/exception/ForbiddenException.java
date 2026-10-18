package com.drivehub.escuela.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, HttpStatus.FORBIDDEN, message);
    }
}
