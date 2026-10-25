package com.drivehub.pagos.features.pagos.pasarela;

/**
 * La devolución que la pasarela creó (CU31). Que esté creada no significa que el dinero ya volvió: eso lo avisa la
 * pasarela después (`REEMBOLSO_EJECUTADO` o `REEMBOLSO_FALLIDO`).
 *
 * @param referenciaExterna el id de la devolución en la pasarela
 * @param estado el estado que dijo la pasarela al crearla (`pending`, `succeeded`...)
 */
public record DevolucionCreada(String referenciaExterna, String estado) {
}
