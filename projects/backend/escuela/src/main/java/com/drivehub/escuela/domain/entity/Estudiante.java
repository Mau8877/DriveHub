package com.drivehub.escuela.domain.entity;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Perfil del estudiante (CU08). Entidad de dominio: no conoce JPA.
 *
 * * `usuarioCodigo` es el codigo_usuario de Identidad (la persona): es el
 * * valor con el que los demás servicios referencian al estudiante.
 */
@Getter
@Builder
public class Estudiante {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final String ci;
    private final LocalDate fechaNacimiento;
    private final String direccion;
    private final CategoriaLicencia categoria;
    private final EstadoEstudiante estado;
    private final LocalDate fechaInscripcion;
    private final String observaciones;
}
