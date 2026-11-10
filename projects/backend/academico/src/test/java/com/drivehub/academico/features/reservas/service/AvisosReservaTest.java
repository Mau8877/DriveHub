package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.common.notificaciones.NotificacionesClient;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Los avisos de una clase que se confirma, se cancela o se reprograma (CU36), y el de la escuela al cancelarse (CU53).
 */
class AvisosReservaTest {

    private final UUID estudiante = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();

    private NotificacionesClient notificaciones;
    private AvisosReserva avisos;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionesClient.class);
        FranjaRepository franjas = mock(FranjaRepository.class);
        Franja franja = mock(Franja.class);
        when(franja.getEscuelaCodigo()).thenReturn(escuela);
        when(franjas.findById(7L)).thenReturn(Optional.of(franja));
        avisos = new AvisosReserva(notificaciones, franjas);
        reserva = new Reserva(estudiante, instructor, 7L, LocalDate.of(2026, 10, 27), "08:00", "09:00", 1,
            Instant.parse("2026-10-26T13:00:00Z"));
        reserva.inicializarCodigo();
    }

    private void verificarAviso(UUID destinatario, String titulo) {
        verify(notificaciones).avisar(any(), eq(destinatario), eq("RESERVA"), eq(titulo), any(), any());
    }

    @Test
    void claseConfirmada_avisaAlEstudianteYAlInstructor() {
        avisos.claseConfirmada(reserva);

        verificarAviso(estudiante, "Clase reservada");
        verificarAviso(instructor, "Nueva clase reservada");
    }

    @Test
    void claseCancelada_porElEstudiante_avisaALosDos() {
        avisos.claseCancelada(reserva, true);

        verificarAviso(estudiante, "Clase cancelada");
        verificarAviso(instructor, "Clase cancelada");
    }

    @Test
    void claseCancelada_porElInstructor_avisaSoloAlEstudiante() {
        ReflectionTestUtils.setField(reserva, "canceladaPorInstructor", true);

        avisos.claseCancelada(reserva, true);

        verificarAviso(estudiante, "Tu instructor canceló la clase");
        verify(notificaciones, never()).avisar(any(), eq(instructor), any(), any(), any(), any());
    }

    @Test
    void claseCancelada_sinHaberseConfirmado_avisaSoloAlEstudiante() {
        avisos.claseCancelada(reserva, false);

        verificarAviso(estudiante, "No pudimos reservar tu clase");
        verify(notificaciones, never()).avisar(any(), eq(instructor), any(), any(), any(), any());
        verify(notificaciones, never()).avisarEscuela(any(), any(), any(), any(), any(), any());
    }

    @Test
    void claseCancelada_confirmada_avisaALaEscuelaDeSuFranja() {
        avisos.claseCancelada(reserva, true);

        verify(notificaciones).avisarEscuela(
            eq(NotificacionesClient.eventoCodigo("clase.cancelada.escuela", reserva.getCodigoReserva())), eq(escuela),
            eq("RESERVA"), eq("Clase cancelada"), eq("El estudiante canceló la clase del 2026-10-27 a las 08:00."),
            any());
    }

    @Test
    void claseCancelada_porElInstructor_tambienAvisaALaEscuela() {
        ReflectionTestUtils.setField(reserva, "canceladaPorInstructor", true);

        avisos.claseCancelada(reserva, true);

        verify(notificaciones).avisarEscuela(any(), eq(escuela), eq("RESERVA"), eq("Clase cancelada"),
            eq("El instructor canceló la clase del 2026-10-27 a las 08:00."), any());
    }

    @Test
    void claseReprogramada_avisaALosDos() {
        avisos.claseReprogramada(reserva);

        verificarAviso(estudiante, "Clase reprogramada");
        verificarAviso(instructor, "Clase reprogramada");
    }

    @Test
    void recordatorio_avisaALosDosConTipoRecordatorio() {
        avisos.recordatorio(reserva);

        verify(notificaciones).avisar(any(), eq(estudiante), eq("RECORDATORIO"), eq("Recuerda tu clase"), any(), any());
        verify(notificaciones).avisar(any(), eq(instructor), eq("RECORDATORIO"), eq("Recuerda tu clase"), any(), any());
    }

    @Test
    void siElClienteFalla_noPropagaElError() {
        doThrow(new IllegalStateException("caído")).when(notificaciones)
            .avisar(any(), any(), any(), any(), any(), any());

        assertThatCode(() -> avisos.claseConfirmada(reserva)).doesNotThrowAnyException();
    }
}
