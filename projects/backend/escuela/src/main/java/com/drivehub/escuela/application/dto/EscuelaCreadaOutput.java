package com.drivehub.escuela.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;

/**
 * La escuela recién creada y la cuenta de su Administrador (CU05).
 */
public record EscuelaCreadaOutput(
        UUID codigo,
        String slug,
        String nombre,
        EstadoEscuela estado,
        Instant creadoEn,
        AdministradorCreado administrador
) {
}
