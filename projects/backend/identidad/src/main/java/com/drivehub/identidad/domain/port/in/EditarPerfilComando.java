package com.drivehub.identidad.domain.port.in;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Datos personales a cambiar (CU06). Cada campo es opcional: vacío significa "no tocar".
 *
 * * `telefono`: presente pero vacío (o en blanco) lo borra.
 * * No lleva el correo, la contraseña ni la foto: tienen sus propios casos de uso.
 */
public record EditarPerfilComando(
        UUID usuarioCodigo,
        Optional<String> nombres,
        Optional<String> apellidos,
        Optional<String> telefono,
        Optional<LocalDate> fechaNacimiento) {
}
