package com.drivehub.academico.common.exception;

/**
 * Catálogo de códigos de error del servicio Académico (prefijo ACA).
 *
 * * Cada código nuevo se documenta en docs/errors/academico/ERRORES_ACADEMICO.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 * * Numeración secuencial dentro del servicio: no se reutiliza un número.
 */
public final class AcademicoErrorCodes {

    // * CU12: horarios del instructor
    public static final String HORA_FIN_NO_POSTERIOR = "ERR_ACA_01";
    public static final String DURACION_INVALIDA = "ERR_ACA_02";
    public static final String VIGENCIA_INVALIDA = "ERR_ACA_03";
    public static final String HORARIO_SE_CRUZA = "ERR_ACA_04";
    public static final String HORARIO_NO_ENCONTRADO = "ERR_ACA_05";
    public static final String HORARIO_YA_INACTIVO = "ERR_ACA_06";
    public static final String FRANJA_NO_ENCONTRADA = "ERR_ACA_07";
    public static final String FRANJA_NO_ESTA_LIBRE = "ERR_ACA_08";
    public static final String FRANJA_NO_ESTA_BLOQUEADA = "ERR_ACA_09";
    public static final String FRANJA_YA_EMPEZO = "ERR_ACA_10";
    public static final String HORARIO_NO_ESTA_ACTIVO = "ERR_ACA_11";
    public static final String HORARIO_NO_ESTA_PAUSADO = "ERR_ACA_12";
    public static final String RANGO_FECHAS_INVALIDO = "ERR_ACA_13";

    // * CU19: banco de preguntas
    public static final String PREGUNTA_NO_ENCONTRADA = "ERR_ACA_14";
    public static final String PREGUNTA_YA_EN_ESE_ESTADO = "ERR_ACA_15";
    public static final String TOKEN_SIN_ESCUELA = "ERR_ACA_16";

    // * Códigos transversales del sistema (ERR_SYS), iguales en todos los servicios
    public static final String VALIDACION = "ERR_SYS_01";
    public static final String SESION_INVALIDA = "ERR_SYS_04";

    private AcademicoErrorCodes() {
    }
}
