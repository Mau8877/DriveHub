package com.drivehub.escuela.domain.entity;

/**
 * Estado de una escuela (tenant). Una escuela INACTIVA no admite registros nuevos.
 */
public enum EstadoEscuela {
    ACTIVA,
    INACTIVA
}
