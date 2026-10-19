package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Dispositivo;

/**
 * Credenciales y plataforma desde la que se inicia sesión (CU02).
 */
public record IniciarSesionComando(String email, String password, Dispositivo dispositivo) {
}
