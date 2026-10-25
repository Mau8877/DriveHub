package com.drivehub.pagos.common.exception;

/**
 * Catálogo de códigos de error de Pagos (`ERR_PAG_NN`, prefijo fijo del servicio). Todo error de
 * negocio que ve el cliente lleva uno de estos y está documentado en `docs/errors/pagos/`.
 *
 * * El número es secuencial dentro del servicio y no se reutiliza aunque el error se retire.
 */
public final class PagosErrorCodes {

    // * CU25: planes de horas
    public static final String PLAN_NOMBRE_EN_USO = "ERR_PAG_01";
    public static final String PLAN_NO_ENCONTRADO = "ERR_PAG_02";
    public static final String PLAN_YA_EN_ESE_ESTADO = "ERR_PAG_03";
    public static final String TOKEN_SIN_ESCUELA = "ERR_PAG_04";

    // * CU26: compra de horas
    public static final String PLAN_NO_DISPONIBLE = "ERR_PAG_05";
    public static final String PASARELA_NO_DISPONIBLE = "ERR_PAG_06";
    public static final String PAGO_NO_ENCONTRADO = "ERR_PAG_07";
    public static final String ALUMNO_NO_ASIGNADO = "ERR_PAG_12";
    public static final String SERVICIO_DEPENDIENTE_NO_DISPONIBLE = "ERR_PAG_15";
    public static final String AVISO_FIRMA_INVALIDA = "ERR_PAG_08";
    public static final String HORAS_INSUFICIENTES = "ERR_PAG_09";
    public static final String CONSUMO_NO_ENCONTRADO = "ERR_PAG_16";
    public static final String COMPRA_EN_CURSO = "ERR_PAG_17";
    public static final String CLAVE_DE_COMPRA_REUTILIZADA = "ERR_PAG_18";

    // * Códigos transversales del sistema (ERR_SYS), iguales en todos los servicios
    public static final String VALIDACION = "ERR_SYS_01";
    public static final String SESION_INVALIDA = "ERR_SYS_04";

    private PagosErrorCodes() {
    }
}
