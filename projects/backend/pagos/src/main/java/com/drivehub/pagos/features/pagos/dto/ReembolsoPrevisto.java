package com.drivehub.pagos.features.pagos.dto;

import java.math.BigDecimal;

/**
 * Cuánto recibiría el estudiante si pide el reembolso de una compra (CU28), en la moneda del pago (USD).
 * `montoDevuelto + montoRetenido` es siempre exactamente lo pagado.
 */
public record ReembolsoPrevisto(BigDecimal montoDevuelto, BigDecimal montoRetenido, int porcentajeRetenido) {
}
