package com.drivehub.academico.features.simulacros.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;

/**
 * Un intento recién iniciado (CU21): las preguntas SIN la respuesta correcta ni la explicación.
 * `venceEn` es cuándo se acaba el tiempo.
 */
public record IntentoIniciadoResponse(UUID codigo, String simulacroNombre, int duracionMin, Instant iniciadoEn,
        Instant venceEn, List<PreguntaParaRendir> preguntas) {

    public record PreguntaParaRendir(UUID codigo, String enunciado, TipoPregunta tipo, List<OpcionParaRendir> opciones) {
    }

    public record OpcionParaRendir(String id, String texto) {
    }
}
