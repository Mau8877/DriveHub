package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /internal/v1/whatsapp/sesiones (CU47). */
public record SesionWhatsappRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{8,15}$", message = "El teléfono no es válido")
        String telefono
) {
}
