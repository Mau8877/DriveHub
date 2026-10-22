package com.drivehub.identidad.domain.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El perfil de la cuenta propia (CU06): lo que la persona ve y puede editar.
 *
 * * No lleva el hash de la contraseña ni ningún id interno.
 * * `escuelaCodigo` y `escuelaNombre` son null en el Super admin; `escuelaNombre` también si
 * * Escuela no respondió. `fotoUrl` es una URL prefirmada de lectura, o null si no tiene foto.
 */
public record PerfilUsuario(
        UUID codigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        boolean correoVerificado,
        List<String> roles,
        UUID escuelaCodigo,
        String escuelaNombre,
        String fotoUrl) {
}
