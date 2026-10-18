package com.drivehub.academico.features.clases.model;

/**
 * Criterio puntuado de una prueba de clase (CU18). `codigo` es la clave legible del criterio, no un UUID.
 */
public record CriterioEvaluacion(String codigo, String descripcion, double puntaje, double maximo) {
}
