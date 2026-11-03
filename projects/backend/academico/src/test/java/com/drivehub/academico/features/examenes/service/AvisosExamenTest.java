package com.drivehub.academico.features.examenes.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.notificaciones.NotificacionesClient;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.model.ExamenConduccion;

/**
 * Los avisos al estudiante por su examen de conducción (CU36).
 */
class AvisosExamenTest {

    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private NotificacionesClient notificaciones;
    private AvisosExamen avisos;
    private ExamenConduccion examen;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionesClient.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-10-26T13:00:00Z"), ZoneId.of("America/La_Paz"));
        avisos = new AvisosExamen(notificaciones, reloj);
        examen = new ExamenConduccion();
        examen.setEstudianteCodigo(ESTUDIANTE);
        examen.setCategoria("B");
        examen.setFechaProgramada(Instant.parse("2026-11-10T14:00:00Z"));
        examen.setEstado(EstadoExamen.PROGRAMADO);
        examen.inicializarCodigo();
    }

    @Test
    void programado_avisaAlEstudianteConLaFechaEnLaHoraDeLaEscuela() {
        avisos.programado(examen);

        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"), eq("Examen de conducción programado"),
            contains("10/11/2026 a las 10:00"), any());
    }

    @Test
    void conResultado_aprobado_felicita() {
        examen.setResultado(ResultadoEvaluacion.APROBADO);
        examen.setPuntaje(85.0);

        avisos.conResultado(examen);

        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"), eq("¡Aprobaste tu examen!"),
            contains("Aprobaste"), any());
    }

    @Test
    void conResultado_reprobado_informa() {
        examen.setResultado(ResultadoEvaluacion.REPROBADO);
        examen.setPuntaje(50.0);

        avisos.conResultado(examen);

        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"), eq("Resultado de tu examen"),
            contains("No aprobaste"), any());
    }

    @Test
    void cancelado_reprogramadoYSinAsistencia_avisanAlEstudiante() {
        avisos.cancelado(examen);
        avisos.reprogramado(examen);
        avisos.sinAsistencia(examen);

        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"), eq("Examen de conducción cancelado"),
            any(), any());
        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"), eq("Examen de conducción reprogramado"),
            any(), any());
        verify(notificaciones).avisar(any(), eq(ESTUDIANTE), eq("EXAMEN"),
            eq("Examen de conducción sin asistencia"), any(), any());
    }

    @Test
    void siElClienteFalla_noPropagaElError() {
        doThrow(new IllegalStateException("caído")).when(notificaciones)
            .avisar(any(), any(), any(), any(), any(), any());

        assertThatCode(() -> avisos.programado(examen)).doesNotThrowAnyException();
    }
}
