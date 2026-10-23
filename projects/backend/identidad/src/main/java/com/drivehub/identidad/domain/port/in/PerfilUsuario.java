package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * El perfil de la cuenta propia (CU06, CU08a): lo que la persona ve y puede editar.
 *
 * * No lleva el hash de la contraseña ni ningún id interno.
 * * `escuelaCodigo` y `escuelaNombre` son null en el Super admin; `escuelaNombre` también si
 * * Escuela no respondió. `fotoUrl` es una URL prefirmada de lectura, o null si no tiene foto.
 * * `categoriaSigla` solo la tiene el estudiante y solo se lee; `direccion` la calcula el servidor.
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
        String fotoUrl,
        String ci,
        SexoPersona sexo,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String categoriaSigla) {
}
