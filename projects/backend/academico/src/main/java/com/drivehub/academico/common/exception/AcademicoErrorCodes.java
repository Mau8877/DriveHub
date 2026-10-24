package com.drivehub.academico.common.exception;

/**
 * Catálogo de códigos de error del servicio Académico (prefijo ACA).
 *
 * * Cada código nuevo se documenta en docs/errors/academico/ERRORES_ACADEMICO.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 */
public final class AcademicoErrorCodes {

    public static final String PREGUNTA_NO_ENCONTRADA = "ERR_ACA_01";
    public static final String PREGUNTA_YA_EN_ESE_ESTADO = "ERR_ACA_02";
    public static final String TOKEN_SIN_ESCUELA = "ERR_ACA_03";

    // * Código transversal del sistema (ERR_SYS), igual en todos los servicios: validación de negocio
    // * que no cubren las anotaciones del DTO.
    public static final String VALIDACION = "ERR_SYS_01";

    private AcademicoErrorCodes() {
    }
}
