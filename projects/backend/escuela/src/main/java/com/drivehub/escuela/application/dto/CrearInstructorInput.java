package com.drivehub.escuela.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos para dar de alta a un instructor (CU09): la persona (va a Identidad) y su ficha profesional
 * (se queda en Escuela).
 *
 * * `escuelaCodigo` sale de la sesión (claim `escuela` del token): nunca del cliente.
 * * `sexo` viaja como texto (`MASCULINO` | `FEMENINO`): el enum es de Identidad.
 * * `fechaContratacion` es opcional (sin ella, hoy). `categorias` son siglas.
 *
 * ! Lleva la contraseña, el CI, el correo, el teléfono y las coordenadas: nunca se escriben en los registros.
 */
public record CrearInstructorInput(
        UUID escuelaCodigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String sexo,
        String ci,
        String telefono,
        String email,
        String password,
        BigDecimal latitud,
        BigDecimal longitud,
        String nroLicencia,
        LocalDate licenciaVence,
        LocalDate fechaContratacion,
        List<String> categorias
) {

    @Override
    public String toString() {
        return "CrearInstructorInput[escuelaCodigo=" + escuelaCodigo + "]";
    }
}
