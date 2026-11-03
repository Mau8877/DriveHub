package com.drivehub.academico.features.reservas.enums;

/**
 * Estado de una reserva (CU14, CU15). PENDIENTE_PAGO vence en expiraPagoEn: la saga la cancela y libera la franja.
 *
 * * `CANCELACION_PENDIENTE` y `REPROGRAMACION_PENDIENTE` son transitorios (CU15): la operación se pidió y todavía no
 * * terminó. Cada uno tiene su manejador en el orquestador de CU44, que los completa o los deshace solo.
 */
public enum EstadoReserva {
    PENDIENTE_PAGO,
    CONFIRMADA,
    CANCELADA,
    COMPLETADA,
    NO_ASISTIO,
    // * El estudiante pidió cancelar; falta devolver la hora (si corresponde), liberar la franja y dejarla CANCELADA
    CANCELACION_PENDIENTE,
    // * El estudiante eligió otra franja; falta terminar el cambio de franja (la clase sigue siendo suya y confirmada)
    REPROGRAMACION_PENDIENTE
}
