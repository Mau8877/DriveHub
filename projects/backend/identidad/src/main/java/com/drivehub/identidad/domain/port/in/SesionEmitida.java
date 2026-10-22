package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Usuario;

/**
 * Sesión recién abierta o renovada.
 *
 * * `fotoUrl` es la URL prefirmada de la foto de la cuenta (CU06), o null si no tiene foto o no se
 * * pudo firmar: un problema del almacenamiento nunca impide iniciar ni renovar la sesión.
 *
 * ! `refreshToken` es el valor en claro: el cliente es el único que lo tiene;
 * ! en la base solo queda su hash. Nunca se loguea.
 */
public record SesionEmitida(String accessToken, long expiraEnSegundos, String refreshToken, Usuario usuario,
        String fotoUrl) {
}
