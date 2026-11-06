package com.drivehub.identidad.domain.port.in;

import java.time.Instant;

/** El enlace de un solo uso para fijar la contraseña y hasta cuándo sirve. */
public record EnlaceRegistro(String enlace, Instant expiraEn) {
}
