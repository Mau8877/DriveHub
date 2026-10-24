package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * Pedir el cambio de correo con sesión (CU06). El correo de la cuenta no cambia hasta confirmar el enlace.
 *
 * ! `passwordActual` nunca se loguea.
 */
public record SolicitarCambioCorreoComando(UUID usuarioCodigo, String passwordActual, String correoNuevo) {
}
