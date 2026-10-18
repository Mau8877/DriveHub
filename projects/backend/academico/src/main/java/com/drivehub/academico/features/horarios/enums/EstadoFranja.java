package com.drivehub.academico.features.horarios.enums;

/**
 * Estado de una franja reservable (CU12, CU14). Reservar = findOneAndUpdate de LIBRE a RESERVADA.
 */
public enum EstadoFranja {
    LIBRE,
    RESERVADA,
    BLOQUEADA
}
