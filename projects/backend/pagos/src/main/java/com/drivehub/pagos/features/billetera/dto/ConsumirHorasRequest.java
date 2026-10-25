package com.drivehub.pagos.features.billetera.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Gastar horas de la billetera de un estudiante (CU27). La escuela sale del token, no del cuerpo.
 */
public record ConsumirHorasRequest(
        @NotNull(message = "Indica el estudiante") UUID estudianteCodigo,
        @NotNull(message = "Indica las horas") @Min(value = 1, message = "Las horas deben ser de 1 a 500")
        @Max(value = 500, message = "Las horas deben ser de 1 a 500") Integer horas,
        @NotNull(message = "Indica el instructor") UUID instructorCodigo,
        @NotNull(message = "Indica la clase") UUID claseCodigo) {
}
