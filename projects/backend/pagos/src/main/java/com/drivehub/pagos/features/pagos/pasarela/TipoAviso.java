package com.drivehub.pagos.features.pagos.pasarela;

/**
 * Qué le avisó la pasarela a Pagos (CU27, CU31).
 *
 * * `COBRO_COMPLETADO`: el estudiante pagó (la sesión de cobro se completó y está pagada).
 * * `COBRO_VENCIDO`: la página de pago venció sin que el estudiante pagara.
 * * `REEMBOLSO_EJECUTADO`: la pasarela devolvió el dinero (CU31).
 * * `REEMBOLSO_FALLIDO`: la pasarela no pudo devolver el dinero (CU31).
 * * `OTRO`: cualquier otro aviso de la pasarela, que Pagos no necesita: se ignora.
 */
public enum TipoAviso {
    COBRO_COMPLETADO,
    COBRO_VENCIDO,
    REEMBOLSO_EJECUTADO,
    REEMBOLSO_FALLIDO,
    OTRO
}
