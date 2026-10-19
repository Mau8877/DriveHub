package com.drivehub.escuela.application.dto;

import java.util.UUID;

/**
 * Datos públicos de una escuela (lo que se puede mostrar sin sesión).
 */
public record EscuelaPublicaOutput(UUID codigo, String slug, String nombre) {
}
