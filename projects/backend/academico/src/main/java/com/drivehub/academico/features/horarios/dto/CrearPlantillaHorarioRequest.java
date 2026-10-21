package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un horario semanal (CU12). Acá solo el formato; las reglas de negocio
 * (hora fin posterior, duración, vigencia, cruces) las valida el service con su código `ERR_ACA_*`.
 *
 * * `diasSemana`: uno o varios días, 1 (lunes) a 7 (domingo); los repetidos se ignoran.
 * * `duracionClaseMin` solo exige estar presente: que esté entre 15 y 240 es regla de negocio.
 * * `vigenteDesde` nulo = hoy.
 */
public record CrearPlantillaHorarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede pasar de 60 caracteres")
        String nombre,

        @NotEmpty(message = "Elige al menos un día de la semana")
        @Size(max = 7, message = "No puede haber más de siete días")
        List<@NotNull(message = "El día de la semana es obligatorio")
             @Min(value = 1, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)")
             @Max(value = 7, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)") Integer> diasSemana,

        @NotBlank(message = "La hora de inicio es obligatoria")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora de inicio debe tener el formato HH:mm")
        String horaInicio,

        @NotBlank(message = "La hora de fin es obligatoria")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora de fin debe tener el formato HH:mm")
        String horaFin,

        @NotNull(message = "La duración de la clase es obligatoria")
        Integer duracionClaseMin,

        LocalDate vigenteDesde,

        LocalDate vigenteHasta) {
}
