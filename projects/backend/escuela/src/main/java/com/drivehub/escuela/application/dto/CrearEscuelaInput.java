package com.drivehub.escuela.application.dto;

import com.drivehub.escuela.domain.gateway.DatosAdministrador;

/**
 * Datos para crear una escuela con su Administrador (CU05).
 *
 * ! `administrador` lleva la contraseña: nunca se escribe en los registros.
 */
public record CrearEscuelaInput(String nombre, String slug, DatosAdministrador administrador) {
}
