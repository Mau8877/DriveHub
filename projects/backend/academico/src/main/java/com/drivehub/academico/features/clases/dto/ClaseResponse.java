package com.drivehub.academico.features.clases.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.model.Clase;

/**
 * Una clase dictada (CU17). Sin ids internos, sin la reserva ni el vehículo.
 *
 * @param codigo el `codigoClase`
 * @param estudianteNombre el nombre completo, o `Estudiante` si Identidad no lo devolvió
 * @param estado siempre `CERRADA`: el estado `ABIERTA` del diseño no se usa
 */
public record ClaseResponse(
        UUID codigo,
        LocalDate fecha,
        UUID estudianteCodigo,
        String estudianteNombre,
        Asistencia asistencia,
        String tema,
        String observaciones,
        Integer kmInicio,
        Integer kmFin,
        EstadoClase estado) {

    public static ClaseResponse de(Clase clase, String estudianteNombre) {
        return new ClaseResponse(clase.getCodigoClase(), clase.getFecha(), clase.getEstudianteCodigo(),
            estudianteNombre, clase.getAsistencia(), clase.getTema(), clase.getObservaciones(), clase.getKmInicio(),
            clase.getKmFin(), clase.getEstado());
    }
}
