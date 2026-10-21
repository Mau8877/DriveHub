package com.drivehub.identidad.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Enlace enviado por correo: restablecer la contraseña (CU04) o verificar el correo (CU04b).
 * Vigencia corta y un solo uso.
 */
@Getter
@Builder
public class TokenRecuperacion {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final TipoTokenCorreo tipo;
    private final String tokenHash;
    private final Instant expiraEn;
    private final boolean usado;
}
