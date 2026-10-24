package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

import com.drivehub.identidad.domain.model.Dispositivo;

/**
 * Cambiar la contraseña con sesión (CU06).
 *
 * * `dispositivo` es el del cliente que pide el cambio: su sesión se conserva y la del otro
 * * dispositivo se cierra (el JWT no dice desde dónde se inició la sesión).
 * ! Las dos contraseñas nunca se loguean.
 */
public record CambiarContrasenaComando(
        UUID usuarioCodigo,
        String passwordActual,
        String passwordNueva,
        Dispositivo dispositivo) {
}
