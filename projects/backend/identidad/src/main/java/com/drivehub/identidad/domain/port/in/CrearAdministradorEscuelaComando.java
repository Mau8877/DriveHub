package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos del Administrador de una escuela que el Super admin crea junto con ella (CU05).
 *
 * * No tiene campo de rol a propósito: el servicio asigna siempre ADMIN_ESCUELA.
 */
public record CrearAdministradorEscuelaComando(
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String password,
        String telefono,
        UUID escuelaCodigo,
        String ci,
        SexoPersona sexo,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
