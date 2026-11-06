package com.drivehub.identidad.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Código de 6 dígitos que se envía al correo para vincular un número (CU47). Solo se guarda su hash.
 *
 * * `usuarioCodigo` es nulo cuando el correo no tenía una cuenta válida: el pedido se registra igual,
 * * ya descartado, para que el límite de pedidos se cuente igual exista o no la cuenta.
 */
@Getter
@Builder
public class DesafioWhatsapp {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final String telefonoHash;
    private final String correoHash;
    private final String codigoHash;
    private final int intentos;
    private final Instant expiraEn;
    private final boolean usado;
}
