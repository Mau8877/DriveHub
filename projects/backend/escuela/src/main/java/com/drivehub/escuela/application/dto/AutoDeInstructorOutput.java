package com.drivehub.escuela.application.dto;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Un auto que un instructor usa hoy (CU11).
 */
public record AutoDeInstructorOutput(UUID vehiculoCodigo, String placa, String marca, String modelo,
        EstadoVehiculo estado) {
}
