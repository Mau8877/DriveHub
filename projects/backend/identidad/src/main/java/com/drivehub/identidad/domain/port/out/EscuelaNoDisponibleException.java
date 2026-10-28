package com.drivehub.identidad.domain.port.out;

import org.springframework.http.HttpStatus;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;

/**
 * Escuela no pudo responder una consulta (caída, timeout o una respuesta inesperada): 503 `ERR_IDE_24`.
 *
 * * Es una excepción de negocio (el dominio puede lanzar las de `common/exception/`): el cliente recibe un
 * * mensaje claro y puede reintentar, en lugar del 500 genérico. El detalle técnico va al registro de quien la
 * * lanza, nunca al mensaje.
 */
public class EscuelaNoDisponibleException extends BusinessException {

    public EscuelaNoDisponibleException() {
        super(IdentidadErrorCodes.ESCUELA_NO_RESPONDE, HttpStatus.SERVICE_UNAVAILABLE,
            "No pudimos consultar la escuela. Inténtalo de nuevo más tarde.");
    }

    public EscuelaNoDisponibleException(Throwable causa) {
        this();
        initCause(causa);
    }
}
