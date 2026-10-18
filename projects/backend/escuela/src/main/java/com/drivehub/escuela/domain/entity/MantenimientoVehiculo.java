package com.drivehub.escuela.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Registro del historial de mantenimiento de un vehículo (CU07).
 */
@Getter
@Builder
public class MantenimientoVehiculo {

    private final UUID codigo;
    private final UUID vehiculoCodigo;
    private final LocalDate fecha;
    private final TipoMantenimiento tipo;
    private final String descripcion;
    private final BigDecimal costo;
    private final Integer kmAlMomento;
}
