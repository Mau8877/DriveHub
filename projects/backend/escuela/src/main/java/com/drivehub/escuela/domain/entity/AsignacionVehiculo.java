package com.drivehub.escuela.domain.entity;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Auto asignado a un instructor (CU10). Un auto tiene una sola asignación
 * vigente (fechaFin null).
 */
@Getter
@Builder
public class AsignacionVehiculo {

    private final UUID codigo;
    private final UUID vehiculoCodigo;
    private final UUID instructorCodigo;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
}
