package com.drivehub.pagos.common.util;

import java.util.UUID;

import com.drivehub.pagos.common.exception.ForbiddenException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;

/**
 * La escuela del token, exigida: la usan los controllers de las pantallas que filtran por escuela (planes,
 * pagos). Sin escuela no hay tenant que filtrar.
 */
public final class EscuelaDelToken {

    private EscuelaDelToken() {
    }

    /**
     * @param escuelaCodigo lo que entregó `@CurrentEscuelaId` (null si el token no la trae)
     * @throws ForbiddenException `ERR_PAG_04` si el token no trae la escuela
     */
    public static UUID exigir(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(PagosErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
        return escuelaCodigo;
    }
}
