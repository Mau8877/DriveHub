package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

/**
 * Contrato HTTP de una escuela con su estado (consulta interna entre servicios).
 */
public record EscuelaEstadoResponse(UUID codigo, String slug, String nombre, boolean activa) {
}
