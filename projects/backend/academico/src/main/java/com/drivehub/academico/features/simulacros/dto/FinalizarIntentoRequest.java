package com.drivehub.academico.features.simulacros.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/intentos-simulacro/{codigo}/finalizar (CU21): lo que marcó el estudiante. Una pregunta que
 * no viaja se toma como sin responder.
 */
public record FinalizarIntentoRequest(
        @NotNull(message = "Las respuestas son obligatorias")
        @Size(max = 100, message = "Demasiadas respuestas")
        @Valid
        List<RespuestaRequest> respuestas
) {

    public record RespuestaRequest(
            @NotNull(message = "La pregunta es obligatoria")
            UUID preguntaCodigo,

            @NotNull(message = "Las opciones elegidas son obligatorias")
            @Size(max = 6, message = "Demasiadas opciones")
            List<String> elegidas
    ) {
    }
}
