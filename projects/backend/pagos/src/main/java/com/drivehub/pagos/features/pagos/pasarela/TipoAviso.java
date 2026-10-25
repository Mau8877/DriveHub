package com.drivehub.pagos.features.pagos.pasarela;

/**
 * Qué le avisó la pasarela a Pagos (CU27).
 *
 * * `COBRO_COMPLETADO`: el estudiante pagó (la sesión de cobro se completó y está pagada).
 * * `COBRO_VENCIDO`: la página de pago venció sin que el estudiante pagara.
 * * `OTRO`: cualquier otro aviso de la pasarela, que Pagos no necesita: se ignora.
 */
public enum TipoAviso {
    COBRO_COMPLETADO,
    COBRO_VENCIDO,
    OTRO
}
