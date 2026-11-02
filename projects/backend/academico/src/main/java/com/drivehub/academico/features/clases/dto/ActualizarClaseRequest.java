package com.drivehub.academico.features.clases.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Corrige una clase ya cerrada (CU17). Todo es opcional y lo que no se envía no se toca; un texto en blanco borra ese
 * texto. La asistencia no se puede cambiar, por eso no está acá.
 *
 * @param tema hasta 120 caracteres; no se admite en una clase `AUSENTE`
 * @param observaciones hasta 500 caracteres
 * @param kmInicio de 0 a 9 999 999; no se admite en una clase `AUSENTE`
 * @param kmFin de 0 a 9 999 999 y no menor que el inicio resultante; no se admite en una clase `AUSENTE`
 */
public record ActualizarClaseRequest(
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
