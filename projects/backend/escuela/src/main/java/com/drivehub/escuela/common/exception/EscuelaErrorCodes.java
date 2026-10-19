package com.drivehub.escuela.common.exception;

/**
 * Catálogo de códigos de error del servicio Escuela (prefijo ESC).
 *
 * * Cada código nuevo se documenta en docs/errors/escuela/ERRORES_ESCUELA.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 */
public final class EscuelaErrorCodes {

    public static final String ESCUELA_NO_ENCONTRADA = "ERR_ESC_01";
    public static final String ESCUELA_NO_ENCONTRADA_POR_CODIGO = "ERR_ESC_02";

    private EscuelaErrorCodes() {
    }
}
