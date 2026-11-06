package com.drivehub.identidad.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se superó un límite de pedidos que Identidad controla por su cuenta (429, `ERR_SYS_02`): las rutas internas
 * de WhatsApp no pasan por el rate limit del gateway.
 */
public class LimiteExcedidoException extends BusinessException {

    public LimiteExcedidoException(String message) {
        super(IdentidadErrorCodes.LIMITE_EXCEDIDO, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
