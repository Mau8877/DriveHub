package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.response.ApiResponse;

/**
 * Cuando la sesión renovable no sirve (ERR_IDE_09), además del 401 estándar
 * borra la cookie `refreshToken` para que el navegador no siga mandándola.
 *
 * * Solo aplica a AuthController. Los demás 401 (credenciales incorrectas) NO
 * * borran la cookie: un intento fallido no debe cerrar una sesión válida.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    private final boolean cookieSegura;

    public AuthExceptionHandler(@Value("${app.cookies.secure:false}") boolean cookieSegura) {
        this.cookieSegura = cookieSegura;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoAutorizado(UnauthorizedException ex) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        if (IdentidadErrorCodes.SESION_INVALIDA.equals(ex.getErrorCode())) {
            ResponseCookie borrada = ResponseCookie.from(AuthController.COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path(AuthController.RUTA_COOKIE)
                .maxAge(0)
                .build();
            respuesta.header(HttpHeaders.SET_COOKIE, borrada.toString());
        }
        return respuesta.body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }
}
