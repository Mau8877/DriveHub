package com.drivehub.academico.features.simulacros.model;

import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;

/**
 * Copia de la pregunta tal como se mostró en un intento (CU21): editar el banco no altera intentos pasados.
 *
 * * `elegidas` son los ids de las opciones que marcó el estudiante (vacío si no respondió); `correcta` se calcula al
 * * finalizar el intento.
 */
public record PreguntaIntento(Long preguntaId, UUID preguntaCodigo, String enunciado, TipoPregunta tipo,
        List<OpcionPregunta> opciones, String explicacion, List<String> elegidas, boolean correcta) {
}
