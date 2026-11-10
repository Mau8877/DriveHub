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

    // * CU14: reservar horario
    public static final String SERVICIO_NO_DISPONIBLE = "ERR_ACA_17";
    public static final String FRANJA_NO_DISPONIBLE = "ERR_ACA_18";
    public static final String HORAS_INSUFICIENTES = "ERR_ACA_19";
    public static final String CLASE_SOLAPADA = "ERR_ACA_20";

    // * CU20: simulacros escritos
    public static final String SIMULACRO_NO_ENCONTRADO = "ERR_ACA_21";
    public static final String SIMULACRO_YA_EN_ESE_ESTADO = "ERR_ACA_22";
    public static final String SIMULACRO_NOMBRE_DUPLICADO = "ERR_ACA_23";

    // * CU22: examen de conducción
    public static final String EXAMEN_NO_ENCONTRADO = "ERR_ACA_24";
    public static final String EXAMEN_NO_ESTA_PROGRAMADO = "ERR_ACA_25";
    public static final String EXAMEN_YA_PROGRAMADO = "ERR_ACA_26";
    public static final String ESTUDIANTE_NO_ENCONTRADO = "ERR_ACA_27";

    // * CU21: rendir simulacro escrito
    public static final String SIMULACRO_SIN_PREGUNTAS = "ERR_ACA_28";
    public static final String INTENTO_EN_CURSO = "ERR_ACA_29";
    public static final String INTENTO_NO_ENCONTRADO = "ERR_ACA_30";
    public static final String INTENTO_YA_FINALIZADO = "ERR_ACA_31";

    // * CU15: cancelar o reprogramar clase
    public static final String RESERVA_NO_ENCONTRADA = "ERR_ACA_32";
    public static final String RESERVA_NO_MODIFICABLE = "ERR_ACA_33";
    public static final String RESERVA_FUERA_DE_PLAZO = "ERR_ACA_34";

    // * CU16: cancelar una clase por el instructor
    public static final String CLASE_INSTRUCTOR_NO_ENCONTRADA = "ERR_ACA_35";
    public static final String CLASE_NO_CANCELABLE_POR_INSTRUCTOR = "ERR_ACA_36";

    // * CU17: cerrar y corregir una clase dictada
    public static final String CLASE_NO_CERRABLE = "ERR_ACA_37";
    public static final String DATOS_DE_CLASE_INVALIDOS = "ERR_ACA_38";

    // * CU18: evaluar una clase dictada (prueba de clase)
    public static final String CLASE_NO_EVALUABLE = "ERR_ACA_39";
    public static final String PRUEBA_YA_REGISTRADA = "ERR_ACA_40";
    public static final String DATOS_DE_PRUEBA_INVALIDOS = "ERR_ACA_41";

    // * CU52: reportes del Administrador
    public static final String RANGO_DE_FECHAS_INVALIDO = "ERR_ACA_42";

    // * Códigos transversales del sistema (ERR_SYS), iguales en todos los servicios
    public static final String VALIDACION = "ERR_SYS_01";
    public static final String SESION_INVALIDA = "ERR_SYS_04";

    private AcademicoErrorCodes() {
    }
}
