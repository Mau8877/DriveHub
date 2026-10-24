package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Datos del alta de la cuenta de un instructor por el Administrador de escuela (CU09).
 *
 * * No tiene campo de rol ni de escuela a propósito: el rol es siempre INSTRUCTOR y la escuela es la
 * * del Administrador (`administradorCodigo`, que sale de la sesión).
 * * `codigo` lo elige Escuela: así puede revertir la cuenta aunque no reciba la respuesta.
 */
public record CrearCuentaInstructorComando(
        UUID administradorCodigo,
        UUID codigo,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        SexoPersona sexo,
        String ci,
        String telefono,
        String email,
        String password,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
