package com.drivehub.academico.features.examenes.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de POST /api/v1/examenes-conduccion (CU22). La escuela NO viaja en el cuerpo: sale del token.
 *
 * * La fecha y la hora viajan separadas (como las edita la pantalla) en la hora de la escuela.
 * * Que la fecha sea futura y que el estudiante sea de la escuela lo valida el servicio.
 */
public record ProgramarExamenRequest(
        @NotNull(message = "El estudiante es obligatorio")
        UUID estudianteCodigo,

        @NotBlank(message = "La categoría es obligatoria")
        @Pattern(regexp = "^[A-Z]{1,3}$", message = "La categoría debe ser una sigla de hasta 3 letras mayúsculas")
        String categoria,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La hora es obligatoria")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe ser HH:mm")
        String hora,

        @NotNull(message = "El evaluador es obligatorio")
        UUID evaluadorCodigo,

        @NotNull(message = "El vehículo es obligatorio")
        UUID vehiculoCodigo
) {
}
