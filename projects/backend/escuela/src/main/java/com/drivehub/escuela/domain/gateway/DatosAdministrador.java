package com.drivehub.escuela.domain.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos del Administrador de la escuela que se va a crear en Identidad (CU05, CU08a).
 *
 * * `sexo` viaja como texto (`MASCULINO` | `FEMENINO`): el enum es de Identidad. La dirección no está:
 * * la calcula Identidad a partir de `latitud` y `longitud`.
 *
 * ! `password` nunca se escribe en los registros.
 */
public record DatosAdministrador(
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String password,
        String telefono,
        String ci,
        String sexo,
        BigDecimal latitud,
        BigDecimal longitud
) {

    @Override
    public String toString() {
        return "DatosAdministrador[email=" + email + ", password=***]";
    }
}
