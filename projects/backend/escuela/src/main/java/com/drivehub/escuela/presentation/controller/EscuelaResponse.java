package com.drivehub.escuela.presentation.controller;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

/**
 * Una escuela en la gestión del Super admin (CU05): también cada elemento del listado.
 */
public record EscuelaResponse(UUID codigo, String slug, String nombre, EstadoEscuela estado, Instant creadoEn) {
}
