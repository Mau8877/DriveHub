package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

public interface QuitarFotoPerfilUseCase {

    /** Borra la foto de la cuenta y devuelve el perfil. Idempotente: sin foto no hace nada. */
    PerfilUsuario quitar(UUID usuarioCodigo);
}
