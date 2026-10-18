package com.drivehub.identidad.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Sesión activa, login único (CU02, CU03).
 *
 * ! Solo se guarda el hash SHA-256 del token: el valor en claro lo tiene
 * ! únicamente el cliente.
 */
@Getter
@Builder
public class RefreshToken {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final String tokenHash;
    private final Dispositivo dispositivo;
    private final Instant expiraEn;
    private final boolean revocado;
}
