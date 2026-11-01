package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Un auto que el instructor usa hoy (CU11). Sin ids internos.
 */
public record AutoDeInstructorResponse(UUID vehiculoCodigo, String placa, String marca, String modelo,
        EstadoVehiculo estado) {
}
