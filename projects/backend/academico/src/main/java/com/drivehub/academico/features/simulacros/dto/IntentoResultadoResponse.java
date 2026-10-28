package com.drivehub.academico.features.simulacros.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.simulacros.enums.EstadoIntento;

/**
 * Un intento terminado con su corrección (CU21): cada pregunta con sus opciones correctas, lo que se eligió y la
 * explicación.
 */
public record IntentoResultadoResponse(UUID codigo, String simulacroNombre, EstadoIntento estado, Instant iniciadoEn,
        Instant finalizadoEn, int aciertos, int total, double puntaje, boolean aprobado, double puntajeAprobacion,
        List<Correccion> preguntas) {

    public record Correccion(UUID codigo, String enunciado, TipoPregunta tipo, List<OpcionCorregida> opciones,
            List<String> elegidas, boolean correcta, String explicacion) {
    }

    public record OpcionCorregida(String id, String texto, boolean correcta) {
    }
}
