package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /internal/v1/whatsapp/registros/validaciones (CU48): un dato del registro a la vez.
 * `campo` es nombres, apellidos, fechaNacimiento (yyyy-MM-dd), sexo, ci, correo o ubicacion («lat,lon»).
 */
public record ValidarDatoRegistroRequest(
        @NotBlank(message = "El campo es obligatorio")
        String campo,

        @NotBlank(message = "El valor es obligatorio")
        String valor
) {
}
