package com.drivehub.academico.features.reservas.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/reservas (CU14): las franjas que el estudiante quiere reservar. El estudiante y su escuela
 * NO viajan en el cuerpo: salen del token.
 *
 * * El tope de 10 es el valor por defecto de `app.reservas.maximo-por-confirmacion`; el servicio vuelve a validarlo
 * * contra la configuración, y los repetidos los rechaza él también.
 */
public record ReservarFranjasRequest(
        @NotEmpty(message = "Elige al menos una clase")
        @Size(max = 10, message = "No puedes reservar más de 10 clases a la vez")
        List<@NotNull(message = "Una de las clases no es válida") UUID> franjaCodigos
) {
}
