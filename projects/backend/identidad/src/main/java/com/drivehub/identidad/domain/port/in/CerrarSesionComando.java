package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Dispositivo;

/**
 * La sesión renovable en claro que se quiere cerrar (de la cookie en la web,
 * del cuerpo en el móvil); puede ser nula si el cliente ya no la tiene.
 */
public record CerrarSesionComando(String refreshToken, Dispositivo dispositivo) {
}
