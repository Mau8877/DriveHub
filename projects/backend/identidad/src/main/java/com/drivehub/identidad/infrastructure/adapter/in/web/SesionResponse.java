package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Sesión abierta o renovada.
 *
 * * `refreshToken` solo viaja en el cuerpo para el móvil; en la web va en una
 * * cookie HttpOnly y este campo se omite.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SesionResponse(String accessToken, long expiraEnSegundos, UsuarioSesionResponse usuario, String refreshToken) {

    public record UsuarioSesionResponse(UUID codigo, String nombres, String apellidos, List<String> roles, UUID escuelaCodigo) {
    }
}
