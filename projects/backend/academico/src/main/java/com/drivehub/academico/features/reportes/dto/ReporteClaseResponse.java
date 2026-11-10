package com.drivehub.academico.features.reportes.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;

/**
 * Una clase en el reporte del Administrador (CU52). Los nombres salen de Identidad; si no responde, quedan vacíos.
 */
public record ReporteClaseResponse(
    UUID codigo,
    LocalDate fecha,
    String horaInicio,
    String horaFin,
    EstadoReserva estado,
    UUID estudianteCodigo,
    String estudianteNombre,
    UUID instructorCodigo,
    String instructorNombre,
    int horasCobradas,
    boolean canceladaPorInstructor) {
}
