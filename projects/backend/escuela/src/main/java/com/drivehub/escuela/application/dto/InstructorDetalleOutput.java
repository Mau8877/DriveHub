package com.drivehub.escuela.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * La ficha completa de un instructor (CU09): la ficha de Escuela y los datos de la persona de
 * Identidad. Sin `usuarioCodigo`, `escuelaCodigo` ni ids internos.
 *
 * * Los datos de la persona (nombres, CI, correo...) son null si Identidad no devolvió a la persona.
 * * `fechaAlta` es el día en que se dio de alta la ficha.
 */
public record InstructorDetalleOutput(
        UUID codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String fotoUrl,
        Integer edad,
        LocalDate fechaNacimiento,
        String sexo,
        String ci,
        String email,
        String telefono,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String nroLicencia,
        LocalDate licenciaVence,
        LocalDate fechaContratacion,
        List<CategoriaOutput> categorias,
        EstadoInstructor estado,
        LocalDate fechaAlta
) {
}
