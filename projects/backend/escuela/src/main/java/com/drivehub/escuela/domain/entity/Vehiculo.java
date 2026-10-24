package com.drivehub.escuela.domain.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Auto de la escuela (CU07).
 *
 * * `escuelaCodigo` es el tenant dueño del auto: toda consulta se filtra por él.
 */
@Getter
@Builder(toBuilder = true)
public class Vehiculo {

    private final UUID codigo;
    private final UUID escuelaCodigo;
    private final String placa;
    private final String marca;
    private final String modelo;
    private final Short anio;
    private final String color;
    private final Transmision transmision;
    private final CategoriaLicencia categoria;
    private final EstadoVehiculo estado;
    private final int kilometraje;
    private final LocalDate soatVence;
    private final LocalDate inspeccionVence;
    private final Instant creadoEn;
}
