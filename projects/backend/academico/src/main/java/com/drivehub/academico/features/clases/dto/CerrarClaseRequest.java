package com.drivehub.academico.features.clases.dto;

import java.util.UUID;

import com.drivehub.academico.features.clases.enums.Asistencia;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cierra una clase que ya empezó (CU17): la asistencia y, si el estudiante asistió, lo que se vio y los kilómetros.
 *
 * @param reservaCodigo el `codigo` de la reserva en la agenda del instructor
 * @param tema opcional, hasta 120 caracteres; no se admite con `AUSENTE`
 * @param observaciones opcional, hasta 500 caracteres
 * @param kmInicio opcional, de 0 a 9 999 999; no se admite con `AUSENTE`
 * @param kmFin opcional, de 0 a 9 999 999 y no menor que `kmInicio`; no se admite con `AUSENTE`
 */
public record CerrarClaseRequest(
        @NotNull(message = "Elige la clase que quieres cerrar") UUID reservaCodigo,
        @NotNull(message = "Indica si el estudiante asistió") Asistencia asistencia,
        @Size(max = ReglasDeClaseLimites.MAX_TEMA, message = "El tema admite hasta 120 caracteres") String tema,
        @Size(max = ReglasDeClaseLimites.MAX_OBSERVACIONES,
            message = "Las observaciones admiten hasta 500 caracteres") String observaciones,
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = ReglasDeClaseLimites.MAX_KILOMETRAJE,
            message = "El kilometraje es demasiado grande") Integer kmInicio,
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = ReglasDeClaseLimites.MAX_KILOMETRAJE,
            message = "El kilometraje es demasiado grande") Integer kmFin) {
}
