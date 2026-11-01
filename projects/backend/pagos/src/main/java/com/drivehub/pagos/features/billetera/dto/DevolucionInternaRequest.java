package com.drivehub.pagos.features.billetera.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Devolver las horas de una clase desde una ruta interna (CU44). No hay token: la escuela y el estudiante viajan en
 * el cuerpo y los manda Académico, leídos de su propia reserva.
 */
public record DevolucionInternaRequest(
        @NotNull(message = "Indica la escuela") UUID escuelaCodigo,
        @NotNull(message = "Indica el estudiante") UUID estudianteCodigo,
        @NotNull(message = "Indica la clase") UUID claseCodigo) {
}
