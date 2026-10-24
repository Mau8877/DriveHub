package com.drivehub.escuela.domain.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de la cuenta del instructor que se va a crear en Identidad (CU09).
 *
 * * `usuarioCodigo` lo elige Escuela: así puede revertir la cuenta aunque no reciba la respuesta.
 * * `sexo` viaja como texto (`MASCULINO` | `FEMENINO`): el enum es de Identidad. La dirección no
 * * está: la calcula Identidad a partir de `latitud` y `longitud`.
 *
 * ! `password`, `ci`, `email`, `telefono` y las coordenadas nunca se escriben en los registros.
 */
public record DatosCuentaInstructor(
        UUID usuarioCodigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String sexo,
        String ci,
        String telefono,
        String email,
        String password,
        BigDecimal latitud,
        BigDecimal longitud
) {

    @Override
    public String toString() {
        return "DatosCuentaInstructor[usuarioCodigo=" + usuarioCodigo + "]";
    }
}
