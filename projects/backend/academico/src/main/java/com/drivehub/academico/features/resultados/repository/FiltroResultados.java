package com.drivehub.academico.features.resultados.repository;

import java.util.UUID;

import com.drivehub.academico.features.resultados.enums.OrigenResultado;

/**
 * Filtros opcionales del listado de resultados (CU23). Todos nulos trae todo lo que la persona puede ver.
 */
public record FiltroResultados(OrigenResultado origen, UUID estudianteCodigo) {
}
