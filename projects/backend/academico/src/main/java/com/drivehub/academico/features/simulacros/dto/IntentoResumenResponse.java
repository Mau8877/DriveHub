package com.drivehub.academico.features.simulacros.dto;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.academico.features.simulacros.enums.EstadoIntento;

/**
 * Un intento en el historial del estudiante (CU21): sin las preguntas.
 */
public record IntentoResumenResponse(UUID codigo, String simulacroNombre, EstadoIntento estado, Instant iniciadoEn,
        Instant finalizadoEn, int aciertos, int total, double puntaje, boolean aprobado) {
}
