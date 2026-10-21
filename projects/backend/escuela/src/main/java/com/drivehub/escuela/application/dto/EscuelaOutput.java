package com.drivehub.escuela.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

/**
 * Una escuela tal como la ve el Super admin al gestionarlas (CU05).
 */
public record EscuelaOutput(UUID codigo, String slug, String nombre, EstadoEscuela estado, Instant creadoEn) {
}
