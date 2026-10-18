package com.drivehub.escuela.domain.entity;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Instructor titular de cada estudiante (CU08, CU29). Un estudiante tiene
 * un solo instructor vigente (fechaFin null).
 */
@Getter
@Builder
public class AsignacionAlumno {

    private final UUID codigo;
    private final UUID instructorCodigo;
    private final UUID estudianteCodigo;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
}
