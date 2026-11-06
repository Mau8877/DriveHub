package com.drivehub.identidad.domain.port.out;

import java.time.Duration;
import java.time.Instant;

import com.drivehub.identidad.domain.model.Usuario;

public interface EmisorTokenAccesoPort {

    /** JWT firmado con los claims de la cuenta; vence a los minutos que dicte la configuración. */
    String emitir(Usuario usuario, Instant emitidoEn);

    /** Lo mismo con una vigencia propia (la sesión corta del chat de WhatsApp, CU47). */
    String emitir(Usuario usuario, Instant emitidoEn, Duration vigencia);
}
