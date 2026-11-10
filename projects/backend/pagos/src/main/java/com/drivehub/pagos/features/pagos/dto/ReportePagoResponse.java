package com.drivehub.pagos.features.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * Una compra de horas en el reporte del Administrador (CU52). El nombre del estudiante es la copia que se guardó al
 * comprar (CU26), así el reporte no depende de Identidad. Nunca lleva el id interno.
 */
public record ReportePagoResponse(
        UUID codigo,
        Instant creadoEn,
        Instant confirmadoEn,
        UUID estudianteCodigo,
        String estudianteNombre,
        String planNombre,
        Short horas,
        Short horasRestantes,
        BigDecimal montoUsd,
        BigDecimal montoBs,
        EstadoPagoVisible estado) {
}
