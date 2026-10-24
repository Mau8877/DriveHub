package com.drivehub.escuela.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Cambios a la ficha profesional y a la ubicación de un instructor (CU09). Todos los campos menos los
 * dos códigos son opcionales: `null` es "no tocar".
 *
 * * No hay campo para ningún dato personal a propósito: nombres, CI, correo, sexo, fecha de
 * * nacimiento y teléfono se corrigen en su propio perfil (CU06), no acá.
 * * `escuelaCodigo` sale de la sesión (claim `escuela` del token): nunca del cliente.
 * * `categorias` son siglas y, si llegan, reemplazan a las que tiene.
 */
public record EditarInstructorInput(
        UUID escuelaCodigo,
        UUID instructorCodigo,
        String nroLicencia,
        LocalDate licenciaVence,
        LocalDate fechaContratacion,
        List<String> categorias,
        EstadoInstructor estado,
        BigDecimal latitud,
        BigDecimal longitud
) {

    @Override
    public String toString() {
        return "EditarInstructorInput[instructorCodigo=" + instructorCodigo + "]";
    }
}
