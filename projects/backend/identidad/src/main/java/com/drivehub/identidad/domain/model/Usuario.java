package com.drivehub.identidad.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Cuenta de acceso (CU01, CU02, CU06). Modelo de dominio: no conoce JPA.
 *
 * * `codigo` es el identificador de persona en TODA la plataforma: el
 * * mismo UUID viaja como usuarioCodigo / estudianteCodigo / instructorCodigo
 * * en los demás microservicios. El id interno (Long) es un detalle del
 * * adaptador de persistencia y no existe en el dominio.
 */
@Getter
@Builder
public class Usuario {

    private final UUID codigo;
    private final String email;
    private final String passwordHash;
    private final String nombres;
    private final String apellidos;
    private final String telefono;
    private final LocalDate fechaNacimiento;
    // * Escuela (tenant) de la cuenta; null para el Super admin
    private final UUID escuelaCodigo;
    private final String fotoUrl;
    private final EstadoUsuario estado;
    private final int intentosFallidos;
    private final Instant ultimoLogin;
    private final Set<Rol> roles;
    private final Instant creadoEn;
    private final Instant actualizadoEn;
}
