package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.drivehub.escuela.common.exception.ForbiddenException;

/**
 * Resuelve `@EscuelaActual UUID` con el claim `escuela` del JWT de la petición.
 *
 * * La escuela sale SIEMPRE del token, nunca de la URL ni del cuerpo: así un Administrador no puede
 * * tocar instructores de otra escuela.
 * * Sin sesión con JWT, sin ese claim (el Super admin no pertenece a ninguna escuela) o con un claim
 * * que no es un UUID, responde 403.
 */
public class EscuelaActualArgumentResolver implements HandlerMethodArgumentResolver {

    // * Mismo código que usa el sistema para un 403 por falta de permiso (ver GlobalExceptionHandler)
    private static final String SIN_PERMISO = "ERR_SYS_05";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(EscuelaActual.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            String escuela = jwt.getToken().getClaimAsString("escuela");
            if (escuela != null) {
                try {
                    return UUID.fromString(escuela);
                } catch (IllegalArgumentException e) {
                    // * Un claim que no es un UUID no es una escuela de esta plataforma
                }
            }
        }
        throw new ForbiddenException(SIN_PERMISO, "No tienes permiso para realizar esta acción");
    }
}
