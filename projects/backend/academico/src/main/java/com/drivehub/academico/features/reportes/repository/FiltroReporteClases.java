package com.drivehub.academico.features.reportes.repository;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;

/**
 * Filtros del reporte de clases de una escuela (CU52). Cualquiera puede venir vacío.
 */
public record FiltroReporteClases(LocalDate desde, LocalDate hasta, EstadoReserva estado, UUID instructorCodigo) {
}
