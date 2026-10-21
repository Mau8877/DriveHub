package com.drivehub.escuela.domain.gateway;

import java.util.UUID;

/**
 * La cuenta del Administrador que Identidad acaba de crear.
 */
public record AdministradorCreado(UUID codigo, String email, String nombres, String apellidos) {
}
