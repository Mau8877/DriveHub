package com.drivehub.academico.features.simulacros.model;

import java.util.List;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;

/**
 * Copia de la pregunta tal como se mostró en un intento (CU21): editar el banco no altera intentos pasados.
 */
public record PreguntaIntento(Long preguntaId, String enunciado, List<OpcionPregunta> opciones, List<String> elegidas, boolean correcta) {
}
