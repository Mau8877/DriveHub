package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos del registro de un estudiante (CU01).
 *
 * * No tiene campo de rol a propósito: el servicio asigna siempre ESTUDIANTE.
 */
public record RegistrarEstudianteComando(
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String password,
        String telefono,
        String escuelaSlug,
        String ci,
        SexoPersona sexo,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
