package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Asignar un auto a un instructor, o cambiarlo a otro (CU10). `fecha` nula = hoy.
 */
public record CambiarAsignacionInput(UUID escuelaCodigo, UUID vehiculoCodigo, UUID instructorCodigo, LocalDate fecha) {
}
