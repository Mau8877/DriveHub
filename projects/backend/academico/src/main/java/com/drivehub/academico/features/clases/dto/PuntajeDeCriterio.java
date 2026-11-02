package com.drivehub.academico.features.clases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * El puntaje que el instructor le puso a un criterio de la prueba (CU18).
 *
 * @param codigo la clave del criterio, tal como la entrega el catálogo
 * @param puntaje de 0 al máximo del criterio, en pasos de 0,5
 */
public record PuntajeDeCriterio(
        @NotBlank(message = "Falta el criterio") String codigo,
        @NotNull(message = "Falta el puntaje del criterio") Double puntaje) {
}
