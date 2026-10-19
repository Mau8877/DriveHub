package com.drivehub.identidad.common.exception;

/**
 * Catálogo de códigos de error del servicio Identidad (prefijo IDE).
 *
 * * Cada código nuevo se documenta en docs/errors/identidad/ERRORES_IDENTIDAD.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 */
public final class IdentidadErrorCodes {

    public static final String CORREO_EN_USO = "ERR_IDE_01";
    public static final String MENOR_DE_EDAD = "ERR_IDE_02";
    public static final String ESCUELA_NO_DISPONIBLE = "ERR_IDE_03";

    private IdentidadErrorCodes() {
    }
}
