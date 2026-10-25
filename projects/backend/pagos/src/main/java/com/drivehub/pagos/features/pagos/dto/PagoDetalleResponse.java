package com.drivehub.pagos.features.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.drivehub.pagos.features.comprobantes.dto.ComprobanteResumen;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * El detalle de una compra (CU28): lo del resumen más las horas que quedan, cuándo se confirmó y si se puede
 * pedir el reembolso. `reembolso` es lo que recibiría el estudiante, o `null` si no es reembolsable.
 * `comprobantes` (CU30) son el de la compra y, si se reembolsó, el del reembolso; vacío si todavía no hay.
 */
public record PagoDetalleResponse(
        UUID codigo,
        String planNombre,
        Short horas,
        BigDecimal montoUsd,
        BigDecimal montoBs,
        EstadoPagoVisible estado,
        Instant creadoEn,
        Short horasRestantes,
        Instant confirmadoEn,
        boolean reembolsable,
        ReembolsoPrevisto reembolso,
        List<ComprobanteResumen> comprobantes) {
}
