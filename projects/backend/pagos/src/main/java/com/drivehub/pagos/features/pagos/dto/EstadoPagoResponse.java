package com.drivehub.pagos.features.pagos.dto;

import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPago;

/**
 * El estado de un pago, para que la app sepa cuándo dejó de estar en proceso (CU26).
 */
public record EstadoPagoResponse(UUID codigo, EstadoPago estado) {
}
