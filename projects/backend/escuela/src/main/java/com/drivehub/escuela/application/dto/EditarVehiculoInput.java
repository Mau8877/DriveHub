package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cambios de un auto (CU07). Un campo nulo no se toca. La placa, la transmisión y el estado no
 * se editan por acá.
 */
public record EditarVehiculoInput(UUID escuelaCodigo, UUID codigo, String marca, String modelo, Short anio,
        String color, Integer kilometraje, LocalDate soatVence, LocalDate inspeccionVence) {
}
