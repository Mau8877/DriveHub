package com.drivehub.escuela.presentation.controller;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

/**
 * La escuela recién creada y la cuenta de su Administrador (CU05). Nunca lleva la contraseña.
 */
public record EscuelaCreadaResponse(
        UUID codigo,
        String slug,
        String nombre,
        EstadoEscuela estado,
        Instant creadoEn,
        AdministradorResponse administrador
) {

    public record AdministradorResponse(UUID codigo, String email, String nombres, String apellidos) {
    }
}
