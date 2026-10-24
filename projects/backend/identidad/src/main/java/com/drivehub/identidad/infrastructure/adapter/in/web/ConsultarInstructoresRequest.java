package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/usuarios/instructores/consulta (CU09): de 1 a 100 códigos de persona.
 */
public record ConsultarInstructoresRequest(
        @NotEmpty(message = "Debes enviar al menos un código")
        @Size(max = 100, message = "No puedes consultar más de 100 códigos a la vez")
        List<UUID> codigos
) {
}
