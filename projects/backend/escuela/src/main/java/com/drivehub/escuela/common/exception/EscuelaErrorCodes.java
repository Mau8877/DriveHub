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
    public static final String SLUG_EN_USO = "ERR_ESC_03";
    public static final String ESCUELA_YA_INACTIVA = "ERR_ESC_04";
    public static final String ESCUELA_YA_ACTIVA = "ERR_ESC_05";
    public static final String ESCUELA_INACTIVA_NO_EDITABLE = "ERR_ESC_06";
    public static final String CATEGORIA_NO_ENCONTRADA = "ERR_ESC_07";
    public static final String NRO_LICENCIA_EN_USO = "ERR_ESC_08";
    public static final String INSTRUCTOR_NO_ENCONTRADO = "ERR_ESC_09";
    public static final String CATEGORIA_INVALIDA = "ERR_ESC_10";

    // * Código transversal del sistema (ERR_SYS), igual en todos los servicios: validación de negocio
    // * que no cubren las anotaciones del DTO.
    public static final String VALIDACION = "ERR_SYS_01";

    private EscuelaErrorCodes() {
    }
}
