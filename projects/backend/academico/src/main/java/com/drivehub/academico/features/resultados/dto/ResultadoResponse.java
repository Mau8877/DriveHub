package com.drivehub.academico.features.resultados.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.resultados.enums.OrigenResultado;

/**
 * Una nota del historial de resultados (CU23): también cada elemento del listado. `dia` es la fecha en la hora de la
 * escuela. `estudianteNombre` solo se completa en la vista del Administrador (el estudiante ya sabe quién es).
 * El id interno Long nunca sale.
 */
public record ResultadoResponse(UUID codigo, UUID estudianteCodigo, String estudianteNombre, OrigenResultado origen,
        String titulo, double puntaje, boolean aprobado, Instant fecha, LocalDate dia) {
}
