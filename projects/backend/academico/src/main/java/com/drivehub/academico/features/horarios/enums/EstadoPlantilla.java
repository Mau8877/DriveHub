package com.drivehub.academico.features.horarios.enums;

/**
 * Estado de un horario semanal (CU12).
 *
 * * El orden de las constantes es el orden en que se listan (activos, pausados, inactivos).
 * * `ACTIVA` y `PAUSADA` se cambian entre sí las veces que haga falta; `INACTIVA` es definitivo.
 */
public enum EstadoPlantilla {
    ACTIVA,
    PAUSADA,
    INACTIVA
}
