package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class CompensadorDeReservaTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private ClienteDePagos cliente;

    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/La_Paz"));

    private CompensadorDeReserva compensador;
    private Franja franja;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        compensador = new CompensadorDeReserva(reservaRepository, franjaRepository, reloj);
        franja = new Franja(escuela, 1L, instructor, LocalDate.of(2026, 10, 27), "08:00", "09:00");
        franja.inicializarCodigo();
        ReflectionTestUtils.setField(franja, "id", 7L);
        franja.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(franja, "reservaId", 100L);
        reserva = new Reserva(estudiante, instructor, 7L, LocalDate.of(2026, 10, 27), "08:00", "09:00", 1, AHORA);
        reserva.inicializarCodigo();
        reserva.setEstado(EstadoReserva.CANCELACION_PENDIENTE);
        ReflectionTestUtils.setField(reserva, "id", 100L);
    }

    private void conLaFranja() {
        when(franjaRepository.findById(7L)).thenReturn(Optional.of(franja));
    }

    private String claveDeDevolucion() {
        return OrquestadorClases.PREFIJO_CLAVE_DEVOLUCION + reserva.getCodigoReserva();
    }

    @Test
    void deshacerDesdeCancelacionPendiente_conDevolucion_devuelveLiberaYCancela() {
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            "Cancelada por el estudiante", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verify(cliente).devolver(escuela, estudiante, reserva.getCodigoReserva(), claveDeDevolucion());
        verify(franjaRepository).liberar(franja.getCodigoFranja(), 100L);
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.CANCELACION_PENDIENTE, EstadoReserva.CANCELADA, "Cancelada por el estudiante", AHORA);
    }

    @Test
    void deshacerSinDevolucion_noLlamaAPagos_peroLiberaYCancela() {
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, false,
            "Cancelada por el estudiante", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verifyNoInteractions(cliente);
        verify(franjaRepository).liberar(franja.getCodigoFranja(), 100L);
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.CANCELACION_PENDIENTE, EstadoReserva.CANCELADA, "Cancelada por el estudiante", AHORA);
    }

    @Test
    void deshacerSinDevolucion_conLaFranjaInexistente_igualCancela() {
        when(franjaRepository.findById(7L)).thenReturn(Optional.empty());

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, false,
            "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verifyNoInteractions(cliente);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.CANCELACION_PENDIENTE, EstadoReserva.CANCELADA, "x", AHORA);
    }

    @Test
    void deshacerConDevolucion_conLaFranjaInexistente_quedaPendienteYNoTocaNada() {
        when(franjaRepository.findById(7L)).thenReturn(Optional.empty());

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.PENDIENTE);
        verifyNoInteractions(cliente);
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void pagosCaidoAlDevolver_devuelvePagosNoRespondeYNoTocaLaFranjaNiElEstado() {
        conLaFranja();
        doThrow(new ServicioNoDisponibleException("ERR_ACA_17", "Pagos no responde")).when(cliente)
            .devolver(any(), any(), any(), anyString());

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void laFranjaQueYaEsDeOtraReserva_noSeLibera() {
        ReflectionTestUtils.setField(franja, "reservaId", 555L);
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void repetirLaCancelacion_usaLaMismaClaveDeDevolucion() {
        conLaFranja();

        compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, "x", cliente);
        compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, "x", cliente);

        // * Pagos no devuelve dos veces con la misma clave; acá se comprueba que siempre se manda la misma
        verify(cliente, org.mockito.Mockito.times(2)).devolver(eq(escuela), eq(estudiante),
            eq(reserva.getCodigoReserva()), eq(claveDeDevolucion()));
    }

    // ---------------------------------------------------------------- bloquear la franja (CU16)

    @Test
    void conBloquearFranja_devuelveLaHoraBloqueaLaFranjaYCancela() {
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            true, "Cancelada por el instructor", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verify(cliente).devolver(escuela, estudiante, reserva.getCodigoReserva(), claveDeDevolucion());
        verify(franjaRepository).bloquearReservada(franja.getCodigoFranja(), 100L);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.CANCELACION_PENDIENTE, EstadoReserva.CANCELADA, "Cancelada por el instructor", AHORA);
    }

    @Test
    void sinBloquearFranja_siguePasandoLaFranjaALibre() {
        conLaFranja();

        compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, false, "x", cliente);

        verify(franjaRepository).liberar(franja.getCodigoFranja(), 100L);
        verify(franjaRepository, never()).bloquearReservada(any(), any());
    }

    @Test
    void conBloquearFranja_pagosCaido_noTocaLaFranjaNiElEstado() {
        conLaFranja();
        doThrow(new ServicioNoDisponibleException("ERR_ACA_17", "Pagos no responde")).when(cliente)
            .devolver(any(), any(), any(), anyString());

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            true, "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);
        verify(franjaRepository, never()).bloquearReservada(any(), any());
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void conBloquearFranja_conLaFranjaYaBloqueada_noLaToca_peroCancela() {
        // * un reintento: la franja ya no es RESERVADA por esta reserva
        franja.setEstado(EstadoFranja.BLOQUEADA);
        ReflectionTestUtils.setField(franja, "reservaId", null);
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            true, "x", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verify(franjaRepository, never()).bloquearReservada(any(), any());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.CANCELACION_PENDIENTE, EstadoReserva.CANCELADA, "x", AHORA);
    }

    @Test
    void elCaminoDeCU14_sigueDeshaciendoDesdePendientePago() {
        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();

        ResultadoOrquestacion resultado = compensador.deshacer(reserva, "Sin horas suficientes", cliente);

        assertThat(resultado).isEqualTo(ResultadoOrquestacion.CANCELADA);
        verify(cliente).devolver(escuela, estudiante, reserva.getCodigoReserva(), claveDeDevolucion());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante, EstadoReserva.PENDIENTE_PAGO,
            EstadoReserva.CANCELADA, "Sin horas suficientes", AHORA);
    }
}
