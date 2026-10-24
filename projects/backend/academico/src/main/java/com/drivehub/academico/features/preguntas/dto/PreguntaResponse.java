package com.drivehub.academico.features.preguntas.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;

/**
 * Una pregunta en la gestión del Administrador de escuela (CU19): también cada elemento del listado.
 * El id interno Long nunca sale.
 */
public record PreguntaResponse(UUID codigo, String categoria, String tema, TipoPregunta tipo, String enunciado,
        List<OpcionResponse> opciones, String explicacion, int dificultad, boolean activa, Instant creadoEn) {

    public record OpcionResponse(String id, String texto, boolean correcta) {
    }
}
