package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

/**
 * Cuenta recién creada. Nunca incluye la contraseña, su hash ni una sesión.
 */
public record UsuarioRegistradoResponse(UUID codigo, String email, String nombres, String apellidos) {
}
