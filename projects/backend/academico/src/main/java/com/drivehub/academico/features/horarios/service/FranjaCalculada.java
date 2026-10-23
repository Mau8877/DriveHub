package com.drivehub.academico.features.horarios.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una franja que corresponde generar para un horario: todavía no es un documento.
 */
public record FranjaCalculada(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
}
