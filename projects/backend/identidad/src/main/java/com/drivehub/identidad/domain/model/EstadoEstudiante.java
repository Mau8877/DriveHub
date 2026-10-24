package com.drivehub.identidad.domain.model;

/**
 * Estado público de un estudiante (CU08): el de su cuenta, tal como lo ve la escuela.
 *
 * * Una cuenta BLOQUEADA temporalmente por intentos fallidos de login (CU02) se muestra ACTIVO:
 * * para la escuela sigue siendo un estudiante activo.
 */
public enum EstadoEstudiante {
    ACTIVO,
    INACTIVO
}
