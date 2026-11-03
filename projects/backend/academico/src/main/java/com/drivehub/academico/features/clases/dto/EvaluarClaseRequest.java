package com.drivehub.academico.features.clases.dto;

import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.clases.enums.TipoPrueba;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Evalúa una clase ya cerrada (CU18): el tipo de prueba y el puntaje de cada uno de sus criterios.
 *
 * @param claseCodigo el `claseCodigo` de la agenda
 * @param criterios exactamente los criterios del tipo, cada uno puntuado
 * @param observaciones opcional, hasta 500 caracteres
 */
public record EvaluarClaseRequest(
        @NotNull(message = "Elige la clase que quieres evaluar") UUID claseCodigo,
        @NotNull(message = "Elige el tipo de prueba") TipoPrueba tipo,
        @NotEmpty(message = "Puntúa los criterios de la prueba") List<@Valid @NotNull(
            message = "Falta un criterio") PuntajeDeCriterio> criterios,
        @Size(max = ReglasDeClaseLimites.MAX_OBSERVACIONES,
            message = "Las observaciones admiten hasta 500 caracteres") String observaciones) {
}
