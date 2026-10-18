package com.drivehub.academico.features.solicitudes.model;

import java.util.List;
import com.drivehub.academico.features.solicitudes.enums.Turno;

/**
 * Preferencias de horario de una solicitud de clase (CU13). dias: 1 (lunes) a 7 (domingo).
 */
public record PreferenciasSolicitud(List<Integer> dias, Turno turno) {
}
