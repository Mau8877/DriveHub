package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * El recordatorio de las clases que empiezan en unas 24 horas (CU36).
 */
class RecordatoriosClaseJobTest {

    // * 2026-10-26 09:30 en La Paz: la ventana es de mañana 08:30 (excluido) a 09:30 (incluido)
    private static final Instant AHORA = Instant.parse("2026-10-26T13:30:00Z");
    private static final LocalDate MANANA = LocalDate.of(2026, 10, 27);

    private ReservaRepository reservaRepository;
    private AvisosReserva avisos;
    private RecordatoriosClaseJob job;

    @BeforeEach
    void preparar() {
        reservaRepository = mock(ReservaRepository.class);
        avisos = mock(AvisosReserva.class);
        Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/La_Paz"));
        job = new RecordatoriosClaseJob(reservaRepository, avisos, reloj, 24);
    }

    private static Reserva reserva(LocalDate fecha, String hora) {
        Reserva r = new Reserva(UUID.randomUUID(), UUID.randomUUID(), 7L, fecha, hora, "10:30", 1, AHORA);
        r.inicializarCodigo();
        return r;
    }

    @Test
    void recuerdaSoloLasClasesQueEmpiezanEnLaUltimaHoraDeLaVentana() {
        Reserva dentro = reserva(MANANA, "09:00");
        Reserva justoEnElLimite = reserva(MANANA, "09:30");
        Reserva antes = reserva(MANANA, "08:30");
        Reserva despues = reserva(MANANA, "10:00");
        when(reservaRepository.confirmadasEnFechas(any(), any(), anyInt()))
            .thenReturn(List.of(antes, dentro, justoEnElLimite, despues));

        int recordadas = job.recordar();

        assertThat(recordadas).isEqualTo(2);
        verify(avisos).recordatorio(dentro);
        verify(avisos).recordatorio(justoEnElLimite);
        verify(avisos, never()).recordatorio(antes);
        verify(avisos, never()).recordatorio(despues);
    }

    @Test
    void consultaDesdeLaFechaDeLaVentanaHastaLaDelLimite() {
        job.recordar();

        verify(reservaRepository).confirmadasEnFechas(MANANA, MANANA, RecordatoriosClaseJob.LIMITE);
    }

    @Test
    void sinClases_noAvisaNada() {
        when(reservaRepository.confirmadasEnFechas(any(), any(), anyInt())).thenReturn(List.of());

        assertThat(job.recordar()).isZero();
        verify(avisos, never()).recordatorio(any());
    }

    @Test
    void siLaBaseFalla_laPasadaNoLanzaNada() {
        when(reservaRepository.confirmadasEnFechas(any(), any(), anyInt())).thenThrow(new IllegalStateException("x"));

        assertThatCode(job::ejecutar).doesNotThrowAnyException();
    }
}
