package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Transmision;

/**
 * Datos para registrar un auto (CU07). `escuelaCodigo` sale del token, no del cuerpo.
 *
 * * `kilometraje` nulo equivale a 0.
 */
public record RegistrarVehiculoInput(UUID escuelaCodigo, String placa, String marca, String modelo, Short anio,
        String color, Transmision transmision, Integer kilometraje, LocalDate soatVence,
        LocalDate inspeccionVence, String imagenUrl) {
}
