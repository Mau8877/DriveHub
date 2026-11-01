package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.MomentoDelDia;

/**
 * Parámetros de la consulta de disponibilidad (CU11). Todos menos la escuela son opcionales.
 *
 * * Valores por defecto: hoy, todo el día, página 1 y 20 por página (máximo 100).
 */
public record ConsultarDisponibilidadInput(UUID escuelaCodigo, Integer pagina, Integer tamano, String busqueda,
        LocalDate fecha, MomentoDelDia momento, Disponibilidad disponibilidad, EstadoAutos estadoAutos) {
}
