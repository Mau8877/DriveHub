package com.drivehub.escuela.application.dto;

import java.util.UUID;

/**
 * Datos de una escuela y si está activa. Lo consulta Identidad para decidir
 * si una cuenta puede iniciar sesión (una escuela suspendida no puede).
 */
public record EscuelaEstadoOutput(UUID codigo, String slug, String nombre, boolean activa) {
}
