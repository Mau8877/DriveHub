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
 * Resuelve `@CurrentEscuelaId UUID`: el claim `escuela` del JWT de la petición.
 *
 * * Las rutas que usan la anotación ya exigen autenticación en `SecurityConfig`; si aun así no hay un
 * * JWT, se responde como una sesión inválida.
 * * Un token sin el claim, o con uno que no es un UUID, da `null`: no es de ninguna escuela de esta
 * * plataforma y quien lo recibe decide qué hacer (el controller responde 403 `ERR_PAG_04`).
 */
public class CurrentUserArgumentResolvers implements HandlerMethodArgumentResolver {

    static final String CLAIM_ESCUELA = "escuela";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentEscuelaId.class)
            && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken jwt)) {
            throw new UnauthorizedException(PagosErrorCodes.SESION_INVALIDA, "Tu sesión terminó");
        }
        String escuela = jwt.getToken().getClaimAsString(CLAIM_ESCUELA);
        if (escuela == null) {
            return null;
        }
        try {
            return UUID.fromString(escuela);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
