package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Dispositivo;

/**
 * La sesión renovable en claro (de la cookie en la web, del cuerpo en el móvil).
 */
public record RenovarSesionComando(String refreshToken, Dispositivo dispositivo) {
}
