package com.drivehub.academico.config;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.UnauthorizedException;

/**
 * Resuelve `@CurrentUserId UUID` (el `sub` del JWT) y `@CurrentEscuelaId UUID`
 * (el claim `escuela`) de la petición.
 *
 * * Las rutas que usan estas anotaciones ya exigen autenticación en `SecurityConfig`;
 * * si aun así no hay un JWT, se responde como una sesión inválida.
 */
public class CurrentUserArgumentResolvers implements HandlerMethodArgumentResolver {

    static final String CLAIM_ESCUELA = "escuela";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean anotado = parameter.hasParameterAnnotation(CurrentUserId.class)
            || parameter.hasParameterAnnotation(CurrentEscuelaId.class);
        return anotado && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken jwt)) {
            throw sesionInvalida();
        }
        try {
            if (parameter.hasParameterAnnotation(CurrentUserId.class)) {
                return UUID.fromString(jwt.getToken().getSubject());
            }
            String escuela = jwt.getToken().getClaimAsString(CLAIM_ESCUELA);
            return escuela == null ? null : UUID.fromString(escuela);
        } catch (IllegalArgumentException | NullPointerException e) {
            // * Un `sub` o una escuela que no son UUID no son de esta plataforma
            throw sesionInvalida();
        }
    }

    private static UnauthorizedException sesionInvalida() {
        return new UnauthorizedException(AcademicoErrorCodes.SESION_INVALIDA, "Tu sesión terminó");
    }
}
