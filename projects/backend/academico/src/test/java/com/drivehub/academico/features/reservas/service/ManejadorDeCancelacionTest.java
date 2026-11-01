package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

@ExtendWith(MockitoExtension.class)
class ManejadorDeCancelacionTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");

    @Mock
    private CompensadorDeReserva compensador;
    @Mock
    private ClienteDePagos cliente;

    private ManejadorDeCancelacion manejador;

    @BeforeEach
    void preparar() {
        manejador = new ManejadorDeCancelacion(compensador);
    }

    private Reserva reserva(boolean sinDevolucion) {
        Reserva reserva = new Reserva(UUID.randomUUID(), UUID.randomUUID(), 7L, LocalDate.of(2026, 10, 27), "08:00",
            "09:00", 1, AHORA);
        reserva.inicializarCodigo();
        reserva.setEstado(EstadoReserva.CANCELACION_PENDIENTE);
        ReflectionTestUtils.setField(reserva, "sinDevolucion", sinDevolucion);
        return reserva;
    }

    @Test
    void declaraSoloElEstadoCancelacionPendiente() {
        assertThat(manejador.estados()).containsExactly(EstadoReserva.CANCELACION_PENDIENTE);
    }

    @Test
    void aTiempo_pideDevolverLaHora_yDevuelveCancelada() {
        Reserva reserva = reserva(false);
        when(compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, "Cancelada por el estudiante",
            cliente)).thenReturn(ResultadoOrquestacion.CANCELADA);

        assertThat(manejador.resolver(reserva, cliente)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(compensador).deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true,
            "Cancelada por el estudiante", cliente);
    }

    @Test
    void tardia_noPideDevolverLaHora() {
        Reserva reserva = reserva(true);
        when(compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, false,
            "Cancelada por el estudiante", cliente)).thenReturn(ResultadoOrquestacion.CANCELADA);

        assertThat(manejador.resolver(reserva, cliente)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(compensador).deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, false,
            "Cancelada por el estudiante", cliente);
    }

    @Test
    void pagosNoResponde_dejaLaReservaPendienteYSeLoDiceAlOrquestador() {
        Reserva reserva = reserva(false);
        when(compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, "Cancelada por el estudiante",
            cliente)).thenReturn(ResultadoOrquestacion.PAGOS_NO_RESPONDE);

        assertThat(manejador.resolver(reserva, cliente)).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);
    }

    @Test
    void usaElClienteRecibido_noUnoPropio() {
        ClienteDePagos otro = mock(ClienteDePagos.class);
        Reserva reserva = reserva(false);
        when(compensador.deshacer(reserva, EstadoReserva.CANCELACION_PENDIENTE, true, "Cancelada por el estudiante",
            otro)).thenReturn(ResultadoOrquestacion.CANCELADA);

        assertThat(manejador.resolver(reserva, otro)).isEqualTo(ResultadoOrquestacion.CANCELADA);
    }
}
