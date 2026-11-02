package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.clases.service.ClaseService;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.dto.CancelacionInstructorResponse;
import com.drivehub.academico.features.reservas.dto.ClaseAgendaResponse;
import com.drivehub.academico.features.reservas.dto.HoraDeCancelacion;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * La autorización por rol NO se ejecuta acá (un servicio sin Spring no corre @PreAuthorize): se comprueba por
 * reflexión que esté declarada.
 */
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final String BEARER = "Bearer token";

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private IdentidadClient identidadClient;
    @Mock
    private ClienteDePagos clienteDeServicio;
    @Mock
    private ClaseService claseService;

    private final UUID instructor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final AtomicLong secuencia = new AtomicLong(100);
    private AgendaService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        OrquestadorProperties orquestadorProps = new OrquestadorProperties(60, 5, true, "0 * * * * *", 50);
        CompensadorDeReserva compensador = new CompensadorDeReserva(reservaRepository, franjaRepository, reloj);
        OrquestadorClases orquestador = new OrquestadorClases(List.of(new ManejadorDeCancelacion(compensador)),
            compensador, reservaRepository, clienteDeServicio, orquestadorProps, reloj);
        PlazoDeClase plazo = new PlazoDeClase(new ReservasProperties(1, 10, 2, 12), reloj);
        service = new AgendaService(reservaRepository, identidadClient, plazo, orquestador, clienteDeServicio,
            claseService, reloj);
        lenient().when(identidadClient.nombresDeEstudiantes(any(), eq(BEARER)))
            .thenReturn(Map.of(estudiante, "Ana Pérez"));
    }

    private Reserva clase(LocalDate fecha, String inicio, String fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(estudiante, instructor, 7L, fecha, inicio, fin, 1,
            AHORA.atZone(ZONA).toInstant());
        reserva.inicializarCodigo();
        reserva.setId(secuencia.incrementAndGet());
        reserva.setEstado(estado);
        return reserva;
    }

    private void laAgendaDevuelve(Reserva... reservas) {
        Page<Reserva> pagina = new PageImpl<>(List.of(reservas), PageRequest.of(0, 20), reservas.length);
        when(reservaRepository.agendaDeInstructor(eq(instructor), any(LocalDate.class), any())).thenReturn(pagina);
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ------------------------------------------------------------------ ver la agenda

    @Test
    void agenda_devuelveLasClasesConElNombreDelEstudiante() {
        Reserva reserva = clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA);
        laAgendaDevuelve(reserva);

        Page<ClaseAgendaResponse> agenda = service.agenda(instructor, escuela, BEARER, null, 1, 20);

        assertThat(agenda.getContent()).hasSize(1);
        ClaseAgendaResponse clase = agenda.getContent().get(0);
        assertThat(clase.codigo()).isEqualTo(reserva.getCodigoReserva());
        assertThat(clase.estudianteCodigo()).isEqualTo(estudiante);
        assertThat(clase.estudianteNombre()).isEqualTo("Ana Pérez");
        assertThat(clase.horaInicio()).isEqualTo("10:00");
        assertThat(clase.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void agenda_sinFecha_pideHoy_yConFechaPideEsaFecha() {
        laAgendaDevuelve();

        service.agenda(instructor, escuela, BEARER, null, 1, 20);
        service.agenda(instructor, escuela, BEARER, "2026-11-03", 1, 20);

        verify(reservaRepository).agendaDeInstructor(eq(instructor), eq(HOY), any());
        verify(reservaRepository).agendaDeInstructor(eq(instructor), eq(LocalDate.of(2026, 11, 3)), any());
    }

    @Test
    void agenda_conUnaFechaInvalida_esERR_SYS_01_ySinConsultarNada() {
        assertError(() -> service.agenda(instructor, escuela, BEARER, "26/10/2026", 1, 20), "ERR_SYS_01");
        assertError(() -> service.agenda(instructor, escuela, BEARER, "2026-13-45", 1, 20), "ERR_SYS_01");

        verifyNoInteractions(reservaRepository, identidadClient);
    }

    @Test
    void agendaYCancelar_sinEscuelaEnElToken_sonERR_ACA_16_ySinConsultarNada() {
        assertError(() -> service.agenda(instructor, null, BEARER, null, 1, 20), "ERR_ACA_16");
        assertError(() -> service.cancelar(instructor, null, BEARER, UUID.randomUUID()), "ERR_ACA_16");

        verifyNoInteractions(reservaRepository, identidadClient, clienteDeServicio);
    }

    @Test
    void agenda_acotaLaPagina() {
        laAgendaDevuelve();

        service.agenda(instructor, escuela, BEARER, null, 0, 5000);

        var captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(reservaRepository).agendaDeInstructor(eq(instructor), eq(HOY), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void agenda_unEstadoTransitorioSeVeComoElDeLaClase() {
        laAgendaDevuelve(clase(HOY, "10:00", "11:00", EstadoReserva.CANCELACION_PENDIENTE),
            clase(HOY, "11:00", "12:00", EstadoReserva.REPROGRAMACION_PENDIENTE));

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        assertThat(contenido.get(0).estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(contenido.get(1).estado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void agenda_cancelableSoloSiEstaConfirmadaYNoEmpezo() {
        laAgendaDevuelve(
            clase(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA),
            clase(HOY.plusDays(1), "08:00", "09:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "12:00", "13:00", EstadoReserva.CANCELADA),
            clase(HOY, "13:00", "14:00", EstadoReserva.COMPLETADA),
            clase(HOY, "14:00", "15:00", EstadoReserva.NO_ASISTIO),
            clase(HOY, "15:00", "16:00", EstadoReserva.REPROGRAMACION_PENDIENTE));

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        // * ya empezó, por empezar (sin plazo de 12 horas), otro día, y las que no están confirmadas
        assertThat(contenido).extracting(ClaseAgendaResponse::cancelable)
            .containsExactly(false, true, true, false, false, false, false);
    }

    @Test
    void agenda_cerrableSoloSiEstaConfirmadaYYaEmpezo_complementoExactoDeCancelable() {
        laAgendaDevuelve(
            clase(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA),
            clase(HOY.plusDays(1), "08:00", "09:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "08:00", "09:00", EstadoReserva.CANCELADA),
            clase(HOY, "08:00", "09:00", EstadoReserva.COMPLETADA),
            clase(HOY, "08:00", "09:00", EstadoReserva.NO_ASISTIO),
            clase(HOY, "08:00", "09:00", EstadoReserva.REPROGRAMACION_PENDIENTE));

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        // * ya empezó, en curso, por empezar, otro día y las que no están confirmadas
        assertThat(contenido).extracting(ClaseAgendaResponse::cerrable)
            .containsExactly(true, true, false, false, false, false, false, false);
        assertThat(contenido).noneMatch(c -> c.cerrable() && c.cancelable());
    }

    @Test
    void agenda_claseCodigoSoloEnLasCerradasQueTienenClase() {
        Reserva completada = clase(HOY, "08:00", "09:00", EstadoReserva.COMPLETADA);
        Reserva noAsistio = clase(HOY, "09:00", "10:00", EstadoReserva.NO_ASISTIO);
        Reserva sinClase = clase(HOY, "10:00", "11:00", EstadoReserva.COMPLETADA);
        Reserva confirmada = clase(HOY, "11:00", "12:00", EstadoReserva.CONFIRMADA);
        Reserva cancelada = clase(HOY, "12:00", "13:00", EstadoReserva.CANCELADA);
        UUID claseA = UUID.randomUUID();
        UUID claseB = UUID.randomUUID();
        // * una clase pendiente de terminar (cierre interrumpido) tampoco se muestra: la reserva sigue confirmada
        laAgendaDevuelve(completada, noAsistio, sinClase, confirmada, cancelada);
        when(claseService.codigosPorReserva(List.of(completada.getId(), noAsistio.getId(), sinClase.getId())))
            .thenReturn(Map.of(completada.getId(), claseA, noAsistio.getId(), claseB, confirmada.getId(),
                UUID.randomUUID()));

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        assertThat(contenido).extracting(ClaseAgendaResponse::claseCodigo)
            .containsExactly(claseA, claseB, null, null, null);
    }

    @Test
    void agenda_pideLosCodigosDeClaseUnaSolaVezYNoSiNoHayCerradas() {
        laAgendaDevuelve(clase(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "09:00", "10:00", EstadoReserva.CANCELADA));

        service.agenda(instructor, escuela, BEARER, null, 1, 20);

        verifyNoInteractions(claseService);

        Reserva cerrada = clase(HOY, "10:00", "11:00", EstadoReserva.COMPLETADA);
        Reserva otra = clase(HOY, "11:00", "12:00", EstadoReserva.NO_ASISTIO);
        laAgendaDevuelve(cerrada, otra);
        when(claseService.codigosPorReserva(any())).thenReturn(Map.of());

        service.agenda(instructor, escuela, BEARER, null, 1, 20);

        verify(claseService).codigosPorReserva(List.of(cerrada.getId(), otra.getId()));
    }

    @Test
    void agenda_terminadaEnElLimiteDeLaHoraDeFin() {
        laAgendaDevuelve(
            clase(HOY, "08:30", "09:30", EstadoReserva.CONFIRMADA),
            clase(HOY, "09:00", "09:31", EstadoReserva.CONFIRMADA),
            clase(HOY.minusDays(1), "09:00", "10:00", EstadoReserva.CONFIRMADA),
            clase(HOY.plusDays(1), "09:00", "10:00", EstadoReserva.CONFIRMADA));

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        assertThat(contenido).extracting(ClaseAgendaResponse::terminada).containsExactly(true, false, true, false);
    }

    @Test
    void agenda_unEstudianteSinNombre_seMuestraComoEstudiante() {
        laAgendaDevuelve(clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA));
        when(identidadClient.nombresDeEstudiantes(any(), eq(BEARER))).thenReturn(Map.of());

        var contenido = service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent();

        assertThat(contenido.get(0).estudianteNombre()).isEqualTo("Estudiante");
    }

    @Test
    void agenda_pideLosNombresUnaSolaVezSinRepetidos() {
        laAgendaDevuelve(clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA),
            clase(HOY, "11:00", "12:00", EstadoReserva.CONFIRMADA));

        service.agenda(instructor, escuela, BEARER, null, 1, 20);

        verify(identidadClient).nombresDeEstudiantes(List.of(estudiante), BEARER);
    }

    @Test
    void agenda_identidadCaida_esERR_ACA_17() {
        laAgendaDevuelve(clase(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA));
        when(identidadClient.nombresDeEstudiantes(any(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "No"));

        assertError(() -> service.agenda(instructor, escuela, BEARER, null, 1, 20), "ERR_ACA_17");
    }

    @Test
    void agenda_sinClases_noLlamaAIdentidad() {
        laAgendaDevuelve();

        assertThat(service.agenda(instructor, escuela, BEARER, null, 1, 20).getContent()).isEmpty();

        verifyNoInteractions(identidadClient);
    }

    // ------------------------------------------------------------------ cancelar (CU16)

    private Reserva claseConfirmada(LocalDate fecha, String inicio, String fin) {
        Reserva reserva = clase(fecha, inicio, fin, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(reserva.getCodigoReserva(),
            instructor)).thenReturn(Optional.of(reserva));
        lenient().when(reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(
            reserva.getCodigoReserva(), estudiante)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    private Franja franjaApartadaPor(Reserva reserva) {
        Franja franja = new Franja(escuela, 1L, instructor, reserva.getFecha(), reserva.getHoraInicio(),
            reserva.getHoraFin());
        franja.inicializarCodigo();
        franja.setEstado(EstadoFranja.RESERVADA);
        franja.setReservaId(reserva.getId());
        lenient().when(franjaRepository.findById(reserva.getFranjaId())).thenReturn(Optional.of(franja));
        return franja;
    }

    private void laCancelacionSeInicia(Reserva reserva) {
        when(reservaRepository.iniciarCancelacionPorInstructor(eq(reserva.getCodigoReserva()), eq(instructor),
            anyString(), any())).thenAnswer(inv -> {
                reserva.setEstado(EstadoReserva.CANCELACION_PENDIENTE);
                ReflectionTestUtils.setField(reserva, "canceladaPorInstructor", true);
                return Optional.of(reserva);
            });
        lenient().when(reservaRepository.cambiarEstado(eq(reserva.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.CANCELACION_PENDIENTE), eq(EstadoReserva.CANCELADA), anyString(), any()))
            .thenAnswer(inv -> {
                reserva.setEstado(EstadoReserva.CANCELADA);
                return Optional.of(reserva);
            });
    }

    @Test
    void cancelar_devuelveLaHora_bloqueaLaFranja_yLaClaseQuedaCancelada() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja franja = franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);

        CancelacionInstructorResponse respuesta = service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
        assertThat(respuesta.clase().estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(respuesta.clase().estudianteNombre()).isEqualTo("Ana Pérez");
        assertThat(respuesta.clase().cancelable()).isFalse();
        verify(clienteDeServicio).devolver(escuela, estudiante, reserva.getCodigoReserva(),
            "devolucion-" + reserva.getCodigoReserva());
        verify(franjaRepository).bloquearReservada(franja.getCodigoFranja(), reserva.getId());
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void cancelar_conMenosDeDoceHoras_igualSeCancelaYSeDevuelveLaHora() {
        // * Faltan 30 minutos: para el estudiante sería tardía, para el instructor no hay plazo
        Reserva reserva = claseConfirmada(HOY, "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);

        CancelacionInstructorResponse respuesta = service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
        verify(clienteDeServicio).devolver(eq(escuela), eq(estudiante), eq(reserva.getCodigoReserva()), anyString());
    }

    @Test
    void cancelar_usaElClienteDeServicio_noElBearerDelInstructor() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);

        service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        verify(clienteDeServicio).devolver(any(), eq(estudiante), any(), anyString());
    }

    @Test
    void cancelar_pagosNoResponde_laCancelacionSeAcepta_yLaHoraQuedaEnProceso() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        doThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No")).when(clienteDeServicio)
            .devolver(any(), any(), any(), anyString());

        CancelacionInstructorResponse respuesta = service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.EN_PROCESO);
        // * sigue CANCELACION_PENDIENTE (el cliente la ve cancelada) y la franja no se tocó todavía
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(respuesta.clase().estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(franjaRepository, never()).bloquearReservada(any(), any());
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void cancelar_unFalloInesperadoAlTerminar_noDeshaceLaCancelacion() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        doThrow(new IllegalStateException("falla inesperada")).when(clienteDeServicio)
            .devolver(any(), any(), any(), anyString());

        CancelacionInstructorResponse respuesta = service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.EN_PROCESO);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
    }

    @Test
    void cancelar_identidadCaida_elNombreSeMuestraComoEstudiante_sinFallar() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(identidadClient.nombresDeEstudiantes(any(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "No"));

        CancelacionInstructorResponse respuesta = service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.clase().estudianteNombre()).isEqualTo("Estudiante");
        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
    }

    @Test
    void cancelar_unaClaseQueNoExisteOEsDeOtroInstructor_esERR_ACA_35() {
        when(reservaRepository.findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(any(), eq(instructor)))
            .thenReturn(Optional.empty());

        assertError(() -> service.cancelar(instructor, escuela, BEARER, UUID.randomUUID()), "ERR_ACA_35");
        verify(reservaRepository, never()).iniciarCancelacionPorInstructor(any(), any(), anyString(), any());
        verifyNoInteractions(clienteDeServicio);
    }

    @Test
    void cancelar_unaClaseQueNoEstaConfirmada_esERR_ACA_36_ySinCambios() {
        for (EstadoReserva estado : List.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA,
                EstadoReserva.COMPLETADA, EstadoReserva.NO_ASISTIO, EstadoReserva.CANCELACION_PENDIENTE,
                EstadoReserva.REPROGRAMACION_PENDIENTE)) {
            Reserva reserva = clase(HOY.plusDays(1), "10:00", "11:00", estado);
            when(reservaRepository.findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(
                reserva.getCodigoReserva(), instructor)).thenReturn(Optional.of(reserva));

            assertError(() -> service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva()), "ERR_ACA_36");
        }
        verify(reservaRepository, never()).iniciarCancelacionPorInstructor(any(), any(), anyString(), any());
        verifyNoInteractions(clienteDeServicio);
    }

    @Test
    void cancelar_unaClaseQueYaEmpezo_esERR_ACA_36() {
        Reserva reserva = claseConfirmada(HOY, "09:00", "10:00");

        assertError(() -> service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva()), "ERR_ACA_36");
        verify(reservaRepository, never()).iniciarCancelacionPorInstructor(any(), any(), anyString(), any());
    }

    @Test
    void cancelar_siOtraPeticionLaCancelaPrimero_laSegundaRecibeERR_ACA_36() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        when(reservaRepository.iniciarCancelacionPorInstructor(eq(reserva.getCodigoReserva()), eq(instructor),
            anyString(), any())).thenReturn(Optional.empty());

        assertError(() -> service.cancelar(instructor, escuela, BEARER, reserva.getCodigoReserva()), "ERR_ACA_36");
        verifyNoInteractions(clienteDeServicio);
    }

    @Test
    void agendaYCancelar_exigenElRolInstructor() throws Exception {
        // * un servicio sin Spring no ejecuta @PreAuthorize: se comprueba que la regla esté declarada
        var agenda = AgendaService.class.getMethod("agenda", UUID.class, UUID.class, String.class, String.class,
            int.class, int.class);
        var cancelar = AgendaService.class.getMethod("cancelar", UUID.class, UUID.class, String.class, UUID.class);

        for (var metodo : List.of(agenda, cancelar)) {
            var regla = metodo.getAnnotation(PreAuthorize.class);
            assertThat(regla).isNotNull();
            assertThat(regla.value()).isEqualTo("hasRole('INSTRUCTOR')");
        }
    }
}
