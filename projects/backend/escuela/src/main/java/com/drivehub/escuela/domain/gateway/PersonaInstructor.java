package com.drivehub.escuela.domain.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La persona de un instructor tal como la guarda Identidad (CU09): cuenta y perfil.
 *
 * * `codigo` es el codigo_usuario de la persona (en la ficha de Escuela: `usuarioCodigo`).
 * * `sexo` viaja como texto (`MASCULINO` | `FEMENINO`): el enum es de Identidad.
 * * `fotoUrl` es la URL prefirmada de lectura (null si no tiene foto).
 */
public record PersonaInstructor(
        UUID codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String fotoUrl,
        int edad,
        LocalDate fechaNacimiento,
        String sexo,
        String ci,
        String email,
        String telefono,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        LocalDate fechaAlta
) {
}
