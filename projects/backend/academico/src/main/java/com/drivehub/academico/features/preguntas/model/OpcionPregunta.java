package com.drivehub.academico.features.preguntas.model;

/**
 * Opción de una pregunta del banco (CU19). `id` es la letra/clave de la opción dentro de la pregunta.
 */
public record OpcionPregunta(String id, String texto, boolean correcta) {
}
