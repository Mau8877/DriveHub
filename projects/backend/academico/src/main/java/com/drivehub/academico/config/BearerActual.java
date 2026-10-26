package com.drivehub.academico.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.UnauthorizedException;

/**
 * El token de la petición que se está atendiendo, listo para reenviarlo (`Authorization: Bearer ...`) a otro
 * servicio por la red interna: el otro servicio vuelve a validar el token y el rol.
 */
@Component
public class BearerActual {

    public String cabecera() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new UnauthorizedException(AcademicoErrorCodes.SESION_INVALIDA, "Tu sesión terminó");
    }
}
