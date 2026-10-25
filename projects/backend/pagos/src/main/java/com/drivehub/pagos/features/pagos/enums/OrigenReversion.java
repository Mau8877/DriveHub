package com.drivehub.pagos.features.pagos.enums;

/**
 * De dónde viene una reversión (CU31).
 *
 * * `SAGA`: compensación automática de un cobro que no se pudo completar; se devuelve el 100 %.
 * * `ESTUDIANTE`: reembolso que pide el estudiante de una compra que no usó; se devuelve lo pagado menos la retención.
 */
public enum OrigenReversion {
    SAGA,
    ESTUDIANTE
}
