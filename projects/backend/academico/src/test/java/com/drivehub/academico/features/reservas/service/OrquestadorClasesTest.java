package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.PagosClient.ResultadoConsumo;
import com.drivehub.academico.features.reservas.client.SinHorasException;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class OrquestadorClasesTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");
    private static final Set<EstadoReserva> PENDIENTES = Set.of(EstadoReserva.PENDIENTE_PAGO);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private ClienteDePagos servicio;
    @Mock
    private ClienteDePagos delEstudiante;

    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/La_Paz"));

    private OrquestadorClases orquestador;
    private Franja franja;

    @BeforeEach
    void preparar() {
        orquestador = orquestadorCon(5);
        franja = new Franja(escuela, 1L, instructor, LocalDate.of(2026, 10, 27), "08:00", "09:00");
        franja.inicializarCodigo();
        ReflectionTestUtils.setField(franja, "id", 7L);
        franja.setEstado(EstadoFranja.RESERVADA);
        ReflectionTestUtils.setField(franja, "reservaId", 100L);
    }

    private OrquestadorClases orquestadorCon(int maximoIntentos) {
        OrquestadorProperties propiedades = new OrquestadorProperties(60, maximoIntentos, true, "0 * * * * *", 50);
        CompensadorDeReserva compensador = new CompensadorDeReserva(reservaRepository, franjaRepository, reloj);
        ManejadorDeReserva manejador = new ManejadorDeReserva(reservaRepository, franjaRepository, compensador,
            propiedades, reloj);
        return new OrquestadorClases(List.of(manejador), compensador, reservaRepository, servicio, propiedades,
            reloj);
    }

    /** Una reserva a medias de {@code minutos} de antigüedad cuya franja es la de este test. */
    private Reserva reserva(long minutos, EstadoReserva estado) {
        Reserva reserva = new Reserva(estudiante, instructor, 7L, LocalDate.of(2026, 10, 27), "08:00", "09:00", 1,
            AHORA);
        reserva.inicializarCodigo();
        reserva.setEstado(estado);
        ReflectionTestUtils.setField(reserva, "id", 100L);
        ReflectionTestUtils.setField(reserva, "createdAt", AHORA.minus(Duration.ofMinutes(minutos)));
        // * lenient: no todos los tests pasan por la relectura (deshacerReserva se llama directo)
        lenient().when(reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(
            reserva.getCodigoReserva(), estudiante)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    private void conLaFranja() {
        when(franjaRepository.findById(7L)).thenReturn(Optional.of(franja));
    }

    // ---------------------------------------------------------------- completar

    @Test
    void reservaReciente_conLaHoraYaDescontada_seConfirmaConLaMismaClave() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(escuela, estudiante, instructor, reserva.getCodigoReserva(), 1,
            "reserva-" + reserva.getCodigoReserva())).thenReturn(new ResultadoConsumo(1, 4));

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, "Reserva retomada por el sistema", AHORA);
        verify(servicio, never()).devolver(any(), any(), any(), anyString());
    }

    @Test
    void reservaReciente_sinHoras_seDeshaceLiberandoLaFranja() throws Exception {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString())).thenThrow(new SinHorasException());

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(servicio).devolver(escuela, estudiante, reserva.getCodigoReserva(),
            "devolucion-" + reserva.getCodigoReserva());
        verify(franjaRepository).liberar(franja.getCodigoFranja(), 100L);
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, "Sin horas suficientes", AHORA);
    }

    @Test
    void pagosCaidoAlDescontar_noTocaNada() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"));

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);

        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), any(), any());
        verify(franjaRepository, never()).liberar(any(), any());
    }

    // ---------------------------------------------------------------- deshacer

    @Test
    void frontera_con59MinutosSeCompleta_yCon60SeDeshace() {
        Reserva de59 = reserva(59, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        assertThat(orquestador.resolver(de59, servicio)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        Reserva de60 = reserva(60, EstadoReserva.PENDIENTE_PAGO);
        assertThat(orquestador.resolver(de60, servicio)).isEqualTo(ResultadoOrquestacion.CANCELADA);
        // * la vieja no intentó descontar de nuevo: solo se devolvió
        verify(servicio, times(1)).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(servicio).devolver(escuela, estudiante, de60.getCodigoReserva(), "devolucion-" + de60.getCodigoReserva());
    }

    @Test
    void reservaVieja_conLaHoraYaDescontada_seDevuelveYSeCancela_noSeConfirma() {
        Reserva reserva = reserva(180, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(servicio, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, "Reserva a medias vencida", AHORA);
    }

    @Test
    void franjaLiberada_devuelveLaHoraYCancela_sinImportarLaEdad() {
        Reserva reserva = reserva(2, EstadoReserva.PENDIENTE_PAGO);
        franja.setEstado(EstadoFranja.LIBRE);
        ReflectionTestUtils.setField(franja, "reservaId", null);
        conLaFranja();

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(servicio).devolver(escuela, estudiante, reserva.getCodigoReserva(),
            "devolucion-" + reserva.getCodigoReserva());
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository).cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, "La franja ya no estaba apartada", AHORA);
    }

    @Test
    void pagosCaidoAlDevolver_noTocaNada() {
        Reserva reserva = reserva(180, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        doThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"))
            .when(servicio).devolver(any(), any(), any(), anyString());

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);

        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), any(), any());
    }

    @Test
    void franjaInexistente_esPendiente() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        when(franjaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.PENDIENTE);

        verifyNoInteractions(servicio);
    }

    @Test
    void deshacerReserva_llamadoDosVeces_devuelveYLiberaUnaSolaVez() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();

        orquestador.deshacerReserva(reserva, "Cancelada", servicio);
        // * la segunda vez la franja ya no es de la reserva y Pagos responde lo mismo (idempotente)
        franja.setEstado(EstadoFranja.LIBRE);
        ReflectionTestUtils.setField(franja, "reservaId", null);
        orquestador.deshacerReserva(reserva, "Cancelada", servicio);

        verify(franjaRepository, times(1)).liberar(franja.getCodigoFranja(), 100L);
        verify(servicio, times(2)).devolver(escuela, estudiante, reserva.getCodigoReserva(),
            "devolucion-" + reserva.getCodigoReserva());
    }

    // ---------------------------------------------------------------- otros estados y manejadores

    @Test
    void unaReservaYaConfirmada_noSeToca_niSeDescuentaDeNuevo() {
        Reserva reserva = reserva(5, EstadoReserva.CONFIRMADA);

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        verifyNoInteractions(servicio);
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), any(), any());
    }

    @Test
    void elOrquestador_nuncaTocaNoAsistioNiCompletadaNiConfirmada() {
        for (EstadoReserva estado : List.of(EstadoReserva.NO_ASISTIO, EstadoReserva.COMPLETADA,
                EstadoReserva.CONFIRMADA)) {
            Reserva reserva = reserva(500, estado);
            orquestador.resolver(reserva, servicio);
        }

        verifyNoInteractions(servicio);
        verifyNoInteractions(franjaRepository);
        assertThat(orquestador.estadosTransitorios()).isEqualTo(PENDIENTES);
    }

    @Test
    void estadosTransitorios_esLaUnionDeLosEstadosDeLosManejadores() {
        ManejadorDeOperacion otro = mock(ManejadorDeOperacion.class);
        when(otro.estados()).thenReturn(Set.of(EstadoReserva.NO_ASISTIO));
        ManejadorDeOperacion primero = mock(ManejadorDeOperacion.class);
        when(primero.estados()).thenReturn(PENDIENTES);

        OrquestadorClases con = new OrquestadorClases(List.of(primero, otro), null, reservaRepository, servicio,
            new OrquestadorProperties(60, 5, true, "0 * * * * *", 50), reloj);

        assertThat(con.estadosTransitorios()).containsExactlyInAnyOrder(EstadoReserva.PENDIENTE_PAGO,
            EstadoReserva.NO_ASISTIO);
    }

    @Test
    void unManejadorNuevo_recibeLasReservasDeSuEstado_conElClienteQueSePasa() {
        ManejadorDeOperacion nuevo = mock(ManejadorDeOperacion.class);
        when(nuevo.estados()).thenReturn(Set.of(EstadoReserva.NO_ASISTIO));
        OrquestadorClases con = new OrquestadorClases(List.of(nuevo), null, reservaRepository, servicio,
            new OrquestadorProperties(60, 5, true, "0 * * * * *", 50), reloj);
        Reserva reserva = reserva(10, EstadoReserva.NO_ASISTIO);
        when(nuevo.resolver(reserva, delEstudiante)).thenReturn(ResultadoOrquestacion.CANCELADA);

        assertThat(con.resolver(reserva, delEstudiante)).isEqualTo(ResultadoOrquestacion.CANCELADA);
    }

    @Test
    void dosManejadoresConElMismoEstado_hacenFallarElArranque() {
        ManejadorDeOperacion a = mock(ManejadorDeOperacion.class);
        ManejadorDeOperacion b = mock(ManejadorDeOperacion.class);
        when(a.estados()).thenReturn(PENDIENTES);
        when(b.estados()).thenReturn(PENDIENTES);

        assertThatThrownBy(() -> new OrquestadorClases(List.of(a, b), null, reservaRepository, servicio,
            new OrquestadorProperties(60, 5, true, "0 * * * * *", 50), reloj))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDIENTE_PAGO");
    }

    @Test
    void elMismoManejadorFuncionaConElClienteDelEstudianteYElDeServicio() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        when(delEstudiante.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));

        assertThat(orquestador.resolver(reserva, servicio)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);
        assertThat(orquestador.resolver(reserva, delEstudiante)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        verify(servicio).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(delEstudiante).descontar(any(), any(), any(), any(), anyInt(), anyString());
    }

    // ---------------------------------------------------------------- intentos y revisión (UT 10)

    private void pagosFallaConUnErrorPropio() {
        // * La franja no existe: causa propia, no de Pagos
        when(franjaRepository.findById(7L)).thenReturn(Optional.empty());
    }

    private Reserva conIntentos(Reserva reserva, int intentos) {
        Reserva actualizada = new Reserva(estudiante, instructor, 7L, LocalDate.of(2026, 10, 27), "08:00", "09:00", 1,
            AHORA);
        ReflectionTestUtils.setField(actualizada, "intentosRetomar", intentos);
        when(reservaRepository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES))
            .thenReturn(Optional.of(actualizada));
        return actualizada;
    }

    @Test
    void procesar_unFalloPropio_sumaUnIntentoYSigueSiendoPendiente() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        pagosFallaConUnErrorPropio();
        conIntentos(reserva, 1);

        assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.PENDIENTE);

        verify(reservaRepository, never()).marcarParaRevision(any(), any(), any(), anySet());
    }

    @Test
    void procesar_alLlegarAlMaximo_marcaParaRevision() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        pagosFallaConUnErrorPropio();
        conIntentos(reserva, 5);
        when(reservaRepository.marcarParaRevision(eq(reserva.getCodigoReserva()),
            eq("No se pudo resolver tras 5 intentos"), eq(AHORA), eq(PENDIENTES))).thenReturn(Optional.of(reserva));

        assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.EN_REVISION);
    }

    @Test
    void procesar_conMaximoUno_marcaAlPrimerFalloPropio() {
        OrquestadorClases estricto = orquestadorCon(1);
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        pagosFallaConUnErrorPropio();
        conIntentos(reserva, 1);
        when(reservaRepository.marcarParaRevision(any(), anyString(), any(), anySet()))
            .thenReturn(Optional.of(reserva));

        assertThat(estricto.procesar(reserva)).isEqualTo(ResultadoOrquestacion.EN_REVISION);
    }

    @Test
    void procesar_confirmarOCancelar_noSumanIntentos() {
        Reserva paraConfirmar = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        assertThat(orquestador.procesar(paraConfirmar)).isEqualTo(ResultadoOrquestacion.CONFIRMADA);

        Reserva paraCancelar = reserva(300, EstadoReserva.PENDIENTE_PAGO);
        assertThat(orquestador.procesar(paraCancelar)).isEqualTo(ResultadoOrquestacion.CANCELADA);

        verify(reservaRepository, never()).registrarIntentoFallido(any(), anySet());
    }

    @Test
    void procesar_pagosNoResponde_nuncaSumaIntentos_aunqueSeRepitaMuchasVeces() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        when(servicio.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"));

        for (int i = 0; i < 20; i++) {
            assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);
        }

        verify(reservaRepository, never()).registrarIntentoFallido(any(), anySet());
        verify(reservaRepository, never()).marcarParaRevision(any(), any(), any(), anySet());
    }

    @Test
    void procesar_conMasDe24HorasYPagosCaido_sigueSinMarcar() {
        Reserva reserva = reserva(60L * 30, EstadoReserva.PENDIENTE_PAGO);
        conLaFranja();
        doThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"))
            .when(servicio).devolver(any(), any(), any(), anyString());

        assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.PAGOS_NO_RESPONDE);

        verify(reservaRepository, never()).marcarParaRevision(any(), any(), any(), anySet());
    }

    @Test
    void procesar_unaExcepcionInesperada_cuentaComoFalloYNoSePropaga() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        when(franjaRepository.findById(7L)).thenThrow(new IllegalStateException("boom"));
        conIntentos(reserva, 1);

        assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.PENDIENTE);

        verify(reservaRepository).registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES);
    }

    @Test
    void procesar_siOtroProcesoYaLaResolvio_noLaMarca() {
        Reserva reserva = reserva(5, EstadoReserva.PENDIENTE_PAGO);
        pagosFallaConUnErrorPropio();
        when(reservaRepository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES))
            .thenReturn(Optional.empty());

        assertThat(orquestador.procesar(reserva)).isEqualTo(ResultadoOrquestacion.PENDIENTE);

        verify(reservaRepository, never()).marcarParaRevision(any(), any(), any(), anySet());
    }
}
