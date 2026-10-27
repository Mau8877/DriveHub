package com.drivehub.pagos.features.billetera.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Descontar horas desde una ruta interna (CU44). No hay token: la escuela y el estudiante viajan en el cuerpo y los
 * manda Académico, leídos de su propia reserva.
 */
public record ConsumoInternoRequest(
        @NotNull(message = "Indica la escuela") UUID escuelaCodigo,
        @NotNull(message = "Indica el estudiante") UUID estudianteCodigo,
        @NotNull(message = "Indica las horas") @Min(value = 1, message = "Las horas deben ser de 1 a 500")
        @Max(value = 500, message = "Las horas deben ser de 1 a 500") Integer horas,
        @NotNull(message = "Indica el instructor") UUID instructorCodigo,
        @NotNull(message = "Indica la clase") UUID claseCodigo) {
}
