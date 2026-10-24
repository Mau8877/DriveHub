package com.drivehub.escuela.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;

/**
 * Un auto tal como lo ve el Administrador de su escuela (CU07).
 */
public record VehiculoOutput(UUID codigo, String placa, String marca, String modelo, Short anio, String color,
        Transmision transmision, EstadoVehiculo estado, int kilometraje, LocalDate soatVence,
        LocalDate inspeccionVence, Instant creadoEn) {
}
