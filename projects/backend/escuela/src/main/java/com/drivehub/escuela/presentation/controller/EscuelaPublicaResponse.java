package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

/**
 * Contrato HTTP de los datos públicos de una escuela.
 */
public record EscuelaPublicaResponse(UUID codigo, String slug, String nombre) {
}
