package com.drivehub.escuela.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Un servicio del que depende la operación no responde (hoy Académico, en la disponibilidad de CU11): se puede
 * reintentar más tarde. Responde 503.
 */
public class ServicioNoDisponibleException extends BusinessException {

    public ServicioNoDisponibleException(String errorCode, String message) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
