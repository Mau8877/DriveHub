package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

/**
 * Administrador de escuela recién creado (CU05). Nunca lleva la contraseña.
 */
public record AdministradorEscuelaResponse(
        UUID codigo,
        String email,
        String nombres,
        String apellidos,
        UUID escuelaCodigo
) {
}
