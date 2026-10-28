package com.drivehub.academico.features.simulacros.dto;

import java.util.UUID;

/**
 * Un simulacro que el estudiante puede rendir (CU21). Sin las reglas por tema: son de la escuela.
 */
public record SimulacroDisponibleResponse(UUID codigo, String nombre, String categoria, int duracionMin,
        int nroPreguntas, double puntajeAprobacion) {
}
