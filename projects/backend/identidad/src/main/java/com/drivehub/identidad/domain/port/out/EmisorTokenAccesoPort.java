package com.drivehub.identidad.domain.port.out;

import java.time.Instant;

import com.drivehub.identidad.domain.model.Usuario;

public interface EmisorTokenAccesoPort {

    /** JWT firmado con los claims de la cuenta; vence a los minutos que dicte la configuración. */
    String emitir(Usuario usuario, Instant emitidoEn);
}
