package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;

/**
 * Liberar un auto (CU10). `fecha` es opcional: hoy por defecto.
 */
public record LiberarVehiculoRequest(LocalDate fecha) {
}
