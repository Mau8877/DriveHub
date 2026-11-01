package com.drivehub.academico.features.simulacros.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Un simulacro en la gestión del Administrador de escuela (CU20): también cada elemento del listado.
 * El id interno Long nunca sale.
 */
public record SimulacroResponse(UUID codigo, String nombre, String categoria, int duracionMin, int nroPreguntas,
        double puntajeAprobacion, List<ReglaResponse> reglas, boolean activo, Instant creadoEn) {

    public record ReglaResponse(String tema, int cantidad) {
    }
}
