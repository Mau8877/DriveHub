package com.drivehub.escuela.presentation.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;

/**
 * Un auto en la gestión del Administrador de escuela (CU07): también cada elemento del listado.
 */
public record VehiculoResponse(UUID codigo, String placa, String marca, String modelo, Short anio, String color,
        Transmision transmision, EstadoVehiculo estado, int kilometraje, LocalDate soatVence,
        LocalDate inspeccionVence, Instant creadoEn) {
}
