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

    // * Códigos transversales del sistema (ERR_SYS), iguales en todos los servicios
    public static final String VALIDACION = "ERR_SYS_01";
    public static final String SESION_INVALIDA = "ERR_SYS_04";

    private PagosErrorCodes() {
    }
}
