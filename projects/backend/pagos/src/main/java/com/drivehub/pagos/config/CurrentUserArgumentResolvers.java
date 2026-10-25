package com.drivehub.pagos.config;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.UnauthorizedException;

/**
 * Resuelve `@CurrentEscuelaId UUID` (el claim `escuela` del JWT) y `@CurrentUserId UUID` (el `sub`, el código de
 * la persona) de la petición.
 *
 * * Las rutas que usan las anotaciones ya exigen autenticación en `SecurityConfig`; si aun así no hay un
 * * JWT, se responde como una sesión inválida.
 * * Un token sin el claim `escuela`, o con uno que no es un UUID, da `null`: no es de ninguna escuela de esta
 * * plataforma y quien lo recibe decide qué hacer (el controller responde 403 `ERR_PAG_04`).
 * * Un token sin un `sub` que sea un UUID no identifica a nadie: es una sesión inválida (401).
 */
public class CurrentUserArgumentResolvers implements HandlerMethodArgumentResolver {

    static final String CLAIM_ESCUELA = "escuela";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return (parameter.hasParameterAnnotation(CurrentEscuelaId.class)
                || parameter.hasParameterAnnotation(CurrentUserId.class))
            && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken jwt)) {
            throw new UnauthorizedException(PagosErrorCodes.SESION_INVALIDA, "Tu sesión terminó");
        }
        if (parameter.hasParameterAnnotation(CurrentUserId.class)) {
            return uuidODefecto(jwt.getToken().getSubject(), true);
        }
        return uuidODefecto(jwt.getToken().getClaimAsString(CLAIM_ESCUELA), false);
    }

    private static UUID uuidODefecto(String texto, boolean obligatorio) {
        try {
            if (texto != null) {
                return UUID.fromString(texto);
            }
        } catch (IllegalArgumentException e) {
            // * No es un UUID: se trata igual que si faltara
        }
        if (obligatorio) {
            throw new UnauthorizedException(PagosErrorCodes.SESION_INVALIDA, "Tu sesión terminó");
        }
        return null;
    }
}
