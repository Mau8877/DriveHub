package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class ManejadorDeReprogramacionTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 28);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private ClienteDePagos cliente;

    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final UUID otroInstructor = UUID.randomUUID();
    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/La_Paz"));

    private ManejadorDeReprogramacion manejador;
    private Franja anterior;
    private Franja destino;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        manejador = new ManejadorDeReprogramacion(reservaRepository, franjaRepository, reloj, mock(AvisosReserva.class));
        anterior = franja(7L, instructor, "08:00", "09:00", EstadoFranja.RESERVADA, 100L);
        destino = franja(8L, otroInstructor, "15:00", "16:00", EstadoFranja.LIBRE, null);
        reserva = new Reserva(estudiante, instructor, 7L, DIA, "08:00", "09:00", 1, AHORA);
        reserva.inicializarCodigo();
        reserva.setEstado(EstadoReserva.REPROGRAMACION_PENDIENTE);
        ReflectionTestUtils.setField(reserva, "id", 100L);
        ReflectionTestUtils.setField(reserva, "franjaDestinoId", 8L);
        lenient().when(franjaRepository.findById(7L)).thenReturn(Optional.of(anterior));
    }

    private Franja franja(long id, UUID instructorFranja, String inicio, String fin, EstadoFranja estado,
            Long reservaId) {
        Franja franja = new Franja(escuela, 1L, instructorFranja, DIA, inicio, fin);
        franja.inicializarCodigo();
        ReflectionTestUtils.setField(franja, "id", id);
        franja.setEstado(estado);
        ReflectionTestUtils.setField(franja, "reservaId", reservaId);
        return franja;
    }

    private void conElDestino() {
        when(franjaRepository.findById(8L)).thenReturn(Optional.of(destino));
    }

    private void verificarQueMovioLaReservaAlDestino() {
        verify(reservaRepository).completarReprogramacion(reserva.getCodigoReserva(), estudiante, 8L, DIA, "15:00",
            "16:00", otroInstructor, AHORA);
    }

    @Test
    void declaraSoloElEstadoReprogramacionPendiente() {
        assertThat(manejador.estados()).containsExactly(EstadoReserva.REPROGRAMACION_PENDIENTE);
    }

    @Test
    void destinoYaApartadaPorLaReserva_liberaLaAnteriorYMueveLaReserva() {
        destino.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(destino, "reservaId", 100L);
        conElDestino();

        ResultadoOrquestacion resultado = manejador.terminar(reserva);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository).liberar(anterior.getCodigoFranja(), 100L);
        verificarQueMovioLaReservaAlDestino();
        verify(franjaRepository, never()).apartar(any(), any(), any(), any(), anyString());
    }

    @Test
    void destinoLibreYApartable_laApartaYMueve() {
        conElDestino();
        when(franjaRepository.apartar(eq(destino.getCodigoFranja()), eq(escuela), eq(100L), any(), anyString()))
            .thenReturn(Optional.of(destino));

        ResultadoOrquestacion resultado = manejador.terminar(reserva);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository).liberar(anterior.getCodigoFranja(), 100L);
        verificarQueMovioLaReservaAlDestino();
    }

    @Test
    void destinoLibrePeroTomadaPorOtro_revierteYLaClaseSigueEnLaOriginal() {
        conElDestino();
        when(franjaRepository.apartar(eq(destino.getCodigoFranja()), eq(escuela), eq(100L), any(), anyString()))
            .thenReturn(Optional.empty());
        when(reservaRepository.revertirReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante), anyString(),
            any())).thenReturn(Optional.of(reserva));

        ResultadoOrquestacion resultado = manejador.terminar(reserva);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).completarReprogramacion(any(), any(), any(), any(), any(), any(), any(),
            any());
        verify(reservaRepository).revertirReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante), anyString(),
            any());
    }

    @Test
    void destinoYaDeOtraReserva_revierteSinTocarLaFranjaAnterior() {
        destino.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(destino, "reservaId", 555L);
        conElDestino();
        when(reservaRepository.revertirReprogramacion(any(), any(), anyString(), any()))
            .thenReturn(Optional.of(reserva));

        ResultadoOrquestacion resultado = manejador.terminar(reserva);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(franjaRepository, never()).apartar(any(), any(), any(), any(), anyString());
    }

    @Test
    void destinoBloqueada_revierte() {
        destino.setEstado(EstadoFranja.BLOQUEADA);
        conElDestino();
        when(reservaRepository.revertirReprogramacion(any(), any(), anyString(), any()))
            .thenReturn(Optional.of(reserva));

        assertThat(manejador.terminar(reserva)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void destinoInexistente_revierte() {
        when(franjaRepository.findById(8L)).thenReturn(Optional.empty());
        when(reservaRepository.revertirReprogramacion(any(), any(), anyString(), any()))
            .thenReturn(Optional.of(reserva));

        assertThat(manejador.terminar(reserva)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void sinFranjaDestinoRegistrada_revierte() {
        ReflectionTestUtils.setField(reserva, "franjaDestinoId", null);
        when(reservaRepository.revertirReprogramacion(any(), any(), anyString(), any()))
            .thenReturn(Optional.of(reserva));

        assertThat(manejador.terminar(reserva)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void siNoSePuedeRevertirPorqueYaNoEstabaAMedias_quedaPendiente() {
        when(franjaRepository.findById(8L)).thenReturn(Optional.empty());
        when(reservaRepository.revertirReprogramacion(any(), any(), anyString(), any())).thenReturn(Optional.empty());

        assertThat(manejador.terminar(reserva)).isEqualTo(ResultadoOrquestacion.PENDIENTE);
    }

    @Test
    void repetirLaTerminacion_noLiberaDeMasPorqueLasOperacionesSonCondicionales() {
        destino.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(destino, "reservaId", 100L);
        conElDestino();

        manejador.terminar(reserva);
        manejador.terminar(reserva);

        verify(franjaRepository, org.mockito.Mockito.times(2)).liberar(anterior.getCodigoFranja(), 100L);
        verify(reservaRepository, org.mockito.Mockito.times(2)).completarReprogramacion(reserva.getCodigoReserva(),
            estudiante, 8L, DIA, "15:00", "16:00", otroInstructor, AHORA);
    }

    @Test
    void resolver_nuncaTocaPagos() {
        destino.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(destino, "reservaId", 100L);
        conElDestino();

        assertThat(manejador.resolver(reserva, cliente)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        verifyNoInteractions(cliente);
    }
}
