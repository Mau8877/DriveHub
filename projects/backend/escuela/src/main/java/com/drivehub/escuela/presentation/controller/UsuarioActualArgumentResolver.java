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
 * Resuelve `@UsuarioActual UUID` con el `sub` del JWT de la petición.
 *
 * * La persona sale SIEMPRE del token, nunca de la URL ni del cuerpo.
 * * Sin sesión con JWT o con un `sub` que no es un UUID, responde 403.
 */
public class UsuarioActualArgumentResolver implements HandlerMethodArgumentResolver {

    // * Mismo código que usa el sistema para un 403 por falta de permiso (ver GlobalExceptionHandler)
    private static final String SIN_PERMISO = "ERR_SYS_05";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioActual.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null) {
            try {
                return UUID.fromString(jwt.getToken().getSubject());
            } catch (IllegalArgumentException e) {
                // * Un sub que no es un UUID no es una persona de esta plataforma
            }
        }
        throw new ForbiddenException(SIN_PERMISO, "No tienes permiso para realizar esta acción");
    }
}
