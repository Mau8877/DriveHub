package com.drivehub.identidad.domain.model;

/**
 * Filtro de estado del listado de estudiantes (CU08).
 *
 * * ACTIVO incluye las cuentas BLOQUEADAS temporalmente por intentos fallidos de login (CU02):
 * * para la escuela siguen siendo estudiantes activos.
 */
public enum EstadoFiltroEstudiante {
    ACTIVO,
    INACTIVO
}
