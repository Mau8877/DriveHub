package com.drivehub.escuela.domain.entity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Perfil del docente (CU09).
 *
 * * `usuarioCodigo` es el codigo_usuario de Identidad (la persona). Su CI y sus datos personales
 * * viven en el perfil de Identidad (CU08a).
 */
@Getter
@Builder
public class Instructor {

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final String nroLicencia;
    private final LocalDate licenciaVence;
    private final EstadoInstructor estado;
    private final LocalDate fechaContratacion;
    private final Set<CategoriaLicencia> categorias;
}
