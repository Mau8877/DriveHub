package com.drivehub.escuela.domain.entity;

/**
 * Estado de una escuela (tenant), DERIVADO de su borrado lógico: ACTIVA si no
 * está eliminada, INACTIVA si lo está. No se guarda en ninguna columna.
 *
 * * Sirve para las salidas y el filtro del listado (CU05). Una escuela
 * * INACTIVA no admite registros ni inicios de sesión de nadie de ella.
 */
public enum EstadoEscuela {
    ACTIVA,
    INACTIVA
}
