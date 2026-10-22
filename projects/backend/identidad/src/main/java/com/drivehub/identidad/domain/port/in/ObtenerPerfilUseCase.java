package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

public interface ObtenerPerfilUseCase {

    /** El perfil de la cuenta con ese codigo (la que tiene la sesión). */
    PerfilUsuario obtener(UUID usuarioCodigo);
}
