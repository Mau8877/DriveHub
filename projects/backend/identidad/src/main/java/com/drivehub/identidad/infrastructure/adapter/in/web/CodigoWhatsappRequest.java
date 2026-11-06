package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de POST /internal/v1/whatsapp/codigos (CU47).
 *
 * * El teléfono es el identificador de WhatsApp: solo dígitos, con o sin «+».
 */
public record CodigoWhatsappRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{8,15}$", message = "El teléfono no es válido")
        String telefono,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        String correo
) {
}
