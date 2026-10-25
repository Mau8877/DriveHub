package com.drivehub.pagos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Un servicio del que depende esta operación no responde (la pasarela de pago, Identidad, Escuela...): 503.
 */
public class ServicioNoDisponibleException extends BusinessException {

    public ServicioNoDisponibleException(String errorCode, String message) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
