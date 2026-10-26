package com.drivehub.academico.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Un servicio del que depende esta operación no responde (Pagos, Identidad): 503. Se puede reintentar más tarde.
 */
public class ServicioNoDisponibleException extends BusinessException {

    public ServicioNoDisponibleException(String errorCode, String message) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
