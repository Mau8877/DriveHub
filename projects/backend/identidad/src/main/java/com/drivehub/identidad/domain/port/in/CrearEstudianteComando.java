package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos del alta de un estudiante por el Administrador de escuela (CU08).
 *
 * * No tiene campo de rol ni de escuela a propósito: el rol es siempre ESTUDIANTE y la escuela es la
 * * del Administrador (`administradorCodigo`, que sale de la sesión).
 * * `categoriaSigla` es opcional: nula o en blanco es "sin categoría".
 */
public record CrearEstudianteComando(
        UUID administradorCodigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        SexoPersona sexo,
        String ci,
        String telefono,
        String email,
        String password,
        BigDecimal latitud,
        BigDecimal longitud,
        String categoriaSigla
) {
}
