package com.drivehub.identidad.infrastructure.adapter.in.web;

import com.drivehub.identidad.domain.port.in.SesionWhatsapp;

/** `data` de POST /internal/v1/whatsapp/sesiones: el token y los segundos que le quedan. */
public record SesionWhatsappResponse(String accessToken, long expiraEn) {

    static SesionWhatsappResponse de(SesionWhatsapp sesion) {
        return new SesionWhatsappResponse(sesion.accessToken(), sesion.expiraEnSegundos());
    }
}
