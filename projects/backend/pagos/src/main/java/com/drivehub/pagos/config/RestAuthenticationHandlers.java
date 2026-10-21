package com.drivehub.pagos.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 y 403 de la capa de seguridad en el sobre estándar de la API.
 *
 * * ERR_SYS_04: no autenticado (falta el token, es inválido o venció).
 * * ERR_SYS_05: autenticado pero sin permiso.
 * * Se escribe a mano: esta capa corre antes de cualquier handler de Spring MVC.
 */
public class RestAuthenticationHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "ERR_SYS_04", "Tu sesión no es válida o venció");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "ERR_SYS_05", "No tienes permiso para realizar esta acción");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String codigo, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":\"failed\",\"message\":\"" + mensaje + "\",\"timestamp\":\""
            + Instant.now() + "\",\"error\":\"" + codigo + "\"}");
    }
}
