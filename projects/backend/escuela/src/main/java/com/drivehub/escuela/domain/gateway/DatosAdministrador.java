package com.drivehub.escuela.domain.gateway;

import java.time.LocalDate;

/**
 * Datos del Administrador de la escuela que se va a crear en Identidad.
 *
 * ! `password` nunca se escribe en los registros.
 */
public record DatosAdministrador(
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String password,
        String telefono
) {

    @Override
    public String toString() {
        return "DatosAdministrador[email=" + email + ", password=***]";
    }
}
