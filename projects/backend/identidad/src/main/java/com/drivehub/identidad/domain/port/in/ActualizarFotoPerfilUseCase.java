package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

public interface ActualizarFotoPerfilUseCase {

    /** Guarda `contenido` como la foto de la cuenta (reemplaza la anterior) y devuelve el perfil. */
    PerfilUsuario actualizar(UUID usuarioCodigo, byte[] contenido);
}
