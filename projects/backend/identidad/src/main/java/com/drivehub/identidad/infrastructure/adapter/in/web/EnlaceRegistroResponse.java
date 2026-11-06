package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.time.Instant;

import com.drivehub.identidad.domain.port.in.EnlaceRegistro;

/** `data` de POST /internal/v1/whatsapp/registros: el enlace para fijar la contraseña y cuándo vence. */
public record EnlaceRegistroResponse(String enlace, Instant expiraEn) {

    static EnlaceRegistroResponse de(EnlaceRegistro enlace) {
        return new EnlaceRegistroResponse(enlace.enlace(), enlace.expiraEn());
    }
}
