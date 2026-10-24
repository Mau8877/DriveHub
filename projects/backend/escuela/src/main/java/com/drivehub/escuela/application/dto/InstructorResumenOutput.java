package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Un instructor en el listado (CU09): la ficha de Escuela y los datos de la persona de Identidad.
 *
 * * `nombreCompleto` y `fotoUrl` son null si Identidad no devolvió a la persona.
 */
public record InstructorResumenOutput(
        UUID codigo,
        String nombreCompleto,
        String fotoUrl,
        String nroLicencia,
        LocalDate licenciaVence,
        List<CategoriaOutput> categorias,
        EstadoInstructor estado
) {
}
