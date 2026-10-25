package com.drivehub.pagos.features.pagos.enums;

/**
 * El estado de un pago tal como lo ve el estudiante (CU28).
 *
 * * Se DERIVA, no se guarda: `REEMBOLSO_EN_PROCESO` es un pago `CONFIRMADO` con una reversión `SOLICITADA` y
 * * `REEMBOLSADO` es un pago `REVERTIDO`. El listado, el detalle y el filtro usan {@link #de}.
 */
public enum EstadoPagoVisible {
    PENDIENTE,
    CONFIRMADO,
    FALLIDO,
    REEMBOLSO_EN_PROCESO,
    REEMBOLSADO;

    /**
     * @param reversionSolicitada si el pago tiene una reversión en estado `SOLICITADA`
     */
    public static EstadoPagoVisible de(EstadoPago estado, boolean reversionSolicitada) {
        return switch (estado) {
            case PENDIENTE -> PENDIENTE;
            case CONFIRMADO -> reversionSolicitada ? REEMBOLSO_EN_PROCESO : CONFIRMADO;
            case FALLIDO -> FALLIDO;
            case REVERTIDO -> REEMBOLSADO;
        };
    }
}
