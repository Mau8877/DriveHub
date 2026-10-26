package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.client.PagosClient;
import com.drivehub.academico.features.reservas.client.PagosClient.ResultadoConsumo;
import com.drivehub.academico.features.reservas.client.SinHorasException;
import com.drivehub.academico.features.reservas.dto.FranjaRechazadaResponse;
import com.drivehub.academico.features.reservas.dto.ReservaResponse;
import com.drivehub.academico.features.reservas.dto.ReservasResultadoResponse;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize).
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

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
    private PagosClient pagosClient;
    @Mock
    private IdentidadClient identidadClient;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final AtomicLong secuencia = new AtomicLong(100);
    private ReservaService service;

    @BeforeEach
    void setUp() {
        service = new ReservaService(reservaRepository, franjaRepository, pagosClient, identidadClient,
            new ReservasProperties(1, 10, 2), Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
        lenient().when(identidadClient.nombresDeInstructores(any(), eq(BEARER)))
            .thenReturn(Map.of(instructor, "Ivan Alfa"));
    }

    private Franja franja(LocalDate fecha, String inicio, String fin) {
        Franja franja = new Franja(escuela, 1L, instructor, fecha, inicio, fin);
        franja.setId(secuencia.incrementAndGet());
        franja.inicializarCodigo();
        return franja;
    }

    /** La franja existe en la escuela, el estudiante no tiene solape y se puede guardar la reserva. */
    private Franja franjaReservable(String inicio) {
        Franja franja = franja(HOY.plusDays(1), inicio, "23:59");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franja.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(franja));
        when(reservaRepository.findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull(franja.getId(),
            estudiante, EstadoReserva.CONFIRMADA)).thenReturn(Optional.empty());
        when(reservaRepository.existeSolape(estudiante, franja.getFecha(), inicio)).thenReturn(false);
        return franja;
    }

    private void seGuardanLasReservasConId() {
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva reserva = inv.getArgument(0);
            reserva.setId(secuencia.incrementAndGet());
            reserva.inicializarCodigo();
            return reserva;
        });
    }

    private void laFranjaSePuedeApartar(Franja franja) {
        when(franjaRepository.apartar(eq(franja.getCodigoFranja()), eq(escuela), any(Long.class), eq(HOY), eq("09:30")))
            .thenReturn(Optional.of(franja));
    }

    private ReservasResultadoResponse reservar(Franja... franjas) {
        return service.reservar(estudiante, escuela, BEARER, java.util.Arrays.stream(franjas)
            .map(Franja::getCodigoFranja).toList());
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---------------------------------------------------------------- camino feliz

    @Test
    void reservar_conHorasSuficientes_apartaLaFranjaDescuentaUnaHoraYConfirma() {
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(pagosClient.descontar(eq(BEARER), eq(estudiante), eq(instructor), any(UUID.class), eq(1), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        when(reservaRepository.cambiarEstado(any(), eq(estudiante), eq(EstadoReserva.PENDIENTE_PAGO),
            eq(EstadoReserva.CONFIRMADA), anyString(), any())).thenAnswer(inv -> {
                Reserva r = new Reserva(estudiante, instructor, franja.getId(), franja.getFecha(), "10:00", "23:59",
                    1, AHORA.atZone(ZONA).toInstant());
                r.setEstado(EstadoReserva.CONFIRMADA);
                return Optional.of(r);
            });
        when(pagosClient.saldo(BEARER)).thenReturn(4);

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.rechazadas()).isEmpty();
        assertThat(resultado.reservadas()).hasSize(1);
        assertThat(resultado.reservadas().get(0).estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(resultado.reservadas().get(0).instructorNombre()).isEqualTo("Ivan Alfa");
        assertThat(resultado.reservadas().get(0).terminada()).isFalse();
        assertThat(resultado.horasDisponibles()).isEqualTo(4);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(guardada.getValue().getHorasCobradas()).isEqualTo(1);
        assertThat(guardada.getValue().getFranjaId()).isEqualTo(franja.getId());
        assertThat(guardada.getValue().getInstructorCodigo()).isEqualTo(instructor);
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void reservar_laClaveDelDescuentoEsEstablePorReserva_yElClaseCodigoEsElCodigoDeLaReserva() {
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        when(reservaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(4);

        reservar(franja);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        UUID codigoReserva = guardada.getValue().getCodigoReserva();
        verify(pagosClient).descontar(BEARER, estudiante, instructor, codigoReserva, 1, "reserva-" + codigoReserva);
    }

    @Test
    void reservar_variasFranjas_descuentaUnaHoraPorCadaUna() {
        Franja a = franjaReservable("10:00");
        Franja b = franjaReservable("11:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(a);
        laFranjaSePuedeApartar(b);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 3));
        when(reservaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(3);

        ReservasResultadoResponse resultado = reservar(a, b);

        assertThat(resultado.reservadas()).hasSize(2);
        assertThat(resultado.rechazadas()).isEmpty();
        verify(pagosClient, times(2)).descontar(any(), any(), any(), any(), eq(1), anyString());
    }

    // ---------------------------------------------------------------- franjas que no se pueden

    @Test
    void reservar_unaFranjaTomadaPorOtro_noDescuentaSuHora_yLasDemasSeReservan() {
        Franja ocupada = franjaReservable("10:00");
        Franja libre = franjaReservable("11:00");
        seGuardanLasReservasConId();
        when(franjaRepository.apartar(eq(ocupada.getCodigoFranja()), eq(escuela), any(Long.class), eq(HOY), eq("09:30")))
            .thenReturn(Optional.empty());
        laFranjaSePuedeApartar(libre);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        when(reservaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(4);

        ReservasResultadoResponse resultado = reservar(ocupada, libre);

        assertThat(resultado.reservadas()).hasSize(1);
        assertThat(resultado.rechazadas()).hasSize(1);
        FranjaRechazadaResponse rechazada = resultado.rechazadas().get(0);
        assertThat(rechazada.franjaCodigo()).isEqualTo(ocupada.getCodigoFranja());
        assertThat(rechazada.error()).isEqualTo("ERR_ACA_18");
        assertThat(rechazada.horaInicio()).isEqualTo("10:00");
        assertThat(rechazada.fecha()).isEqualTo(ocupada.getFecha());
        assertThat(rechazada.instructorNombre()).isEqualTo("Ivan Alfa");
        // * la reserva que se había creado para la franja ocupada se borra, y solo se descontó una hora
        verify(reservaRepository).delete(any(Reserva.class));
        verify(pagosClient, times(1)).descontar(any(), any(), any(), any(), anyInt(), anyString());
    }

    @Test
    void reservar_unaFranjaInexistenteODeOtraEscuela_seRechazaConErrAca18SinDetalles() {
        UUID codigo = UUID.randomUUID();
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(codigo, escuela))
            .thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(5);

        ReservasResultadoResponse resultado = service.reservar(estudiante, escuela, BEARER, List.of(codigo));

        assertThat(resultado.reservadas()).isEmpty();
        assertThat(resultado.rechazadas()).singleElement().satisfies(r -> {
            assertThat(r.error()).isEqualTo("ERR_ACA_18");
            assertThat(r.fecha()).isNull();
            assertThat(r.horaInicio()).isNull();
        });
        verifyNoInteractions(reservaRepository);
        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
    }

    @Test
    void reservar_unaFranjaQueYaEmpezo_seRechazaConErrAca10() {
        Franja empezada = franja(HOY, "09:00", "10:00");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(empezada.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(empezada));
        Franja justoAhora = franja(HOY, "09:30", "10:30");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(justoAhora.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(justoAhora));
        Franja deAyer = franja(HOY.minusDays(1), "15:00", "16:00");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(deAyer.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(deAyer));
        when(pagosClient.saldo(BEARER)).thenReturn(5);

        ReservasResultadoResponse resultado = reservar(empezada, justoAhora, deAyer);

        assertThat(resultado.rechazadas()).hasSize(3).allSatisfy(r -> assertThat(r.error()).isEqualTo("ERR_ACA_10"));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void reservar_unaClaseALaMismaHora_seRechazaConErrAca20() {
        Franja franja = franja(HOY.plusDays(1), "10:00", "11:00");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franja.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(franja));
        when(reservaRepository.findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(reservaRepository.existeSolape(estudiante, franja.getFecha(), "10:00")).thenReturn(true);
        when(pagosClient.saldo(BEARER)).thenReturn(5);

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.rechazadas()).singleElement().satisfies(r -> assertThat(r.error()).isEqualTo("ERR_ACA_20"));
        verify(reservaRepository, never()).save(any());
        verify(franjaRepository, never()).apartar(any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------- horas

    @Test
    void reservar_siSeAcabanLasHorasALaMitad_reservaLasQueAlcanzan_yLiberaLaOtra() {
        Franja primera = franjaReservable("10:00");
        Franja segunda = franjaReservable("11:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(primera);
        laFranjaSePuedeApartar(segunda);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 0)).thenThrow(new SinHorasException());
        when(reservaRepository.cambiarEstado(any(), any(), eq(EstadoReserva.PENDIENTE_PAGO),
            eq(EstadoReserva.CONFIRMADA), anyString(), any())).thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(0);

        ReservasResultadoResponse resultado = reservar(primera, segunda);

        assertThat(resultado.reservadas()).hasSize(1);
        assertThat(resultado.rechazadas()).singleElement().satisfies(r -> {
            assertThat(r.franjaCodigo()).isEqualTo(segunda.getCodigoFranja());
            assertThat(r.error()).isEqualTo("ERR_ACA_19");
        });
        verify(franjaRepository).liberar(eq(segunda.getCodigoFranja()), any(Long.class));
        verify(franjaRepository, never()).liberar(eq(primera.getCodigoFranja()), any());
        verify(reservaRepository).cambiarEstado(any(), eq(estudiante), eq(EstadoReserva.PENDIENTE_PAGO),
            eq(EstadoReserva.CANCELADA), eq("Sin horas suficientes"), any());
        assertThat(resultado.horasDisponibles()).isZero();
    }

    // ---------------------------------------------------------------- Pagos no responde

    @Test
    void reservar_siPagosNoRespondeAlDescontar_devuelveLaHoraLiberaLaFranjaYCancela() {
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));
        when(pagosClient.saldo(BEARER)).thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.reservadas()).isEmpty();
        assertThat(resultado.rechazadas()).singleElement().satisfies(r -> assertThat(r.error()).isEqualTo("ERR_ACA_17"));
        assertThat(resultado.horasDisponibles()).isNull();

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        UUID codigoReserva = guardada.getValue().getCodigoReserva();
        verify(pagosClient).devolver(BEARER, estudiante, codigoReserva, "devolucion-" + codigoReserva);
        verify(franjaRepository).liberar(eq(franja.getCodigoFranja()), any(Long.class));
        verify(reservaRepository).cambiarEstado(eq(codigoReserva), eq(estudiante), eq(EstadoReserva.PENDIENTE_PAGO),
            eq(EstadoReserva.CANCELADA), anyString(), any());
    }

    @Test
    void reservar_siPagosTampocoRespondeAlDevolver_laReservaQuedaPendienteConLaFranjaApartada() {
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));
        doThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No")).when(pagosClient)
            .devolver(any(), any(), any(), anyString());
        when(pagosClient.saldo(BEARER)).thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.rechazadas()).singleElement().satisfies(r -> assertThat(r.error()).isEqualTo("ERR_ACA_17"));
        // * no se libera ni se cancela: la retoma el estudiante al consultar sus clases
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), eq(EstadoReserva.CANCELADA), any(), any());
    }

    // ---------------------------------------------------------------- repetir la confirmación

    @Test
    void reservar_unaFranjaQueYaEsDelEstudiante_cuentaComoReservadaSinDescontarDeNuevo() {
        Franja franja = franja(HOY.plusDays(1), "10:00", "11:00");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franja.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(franja));
        Reserva existente = new Reserva(estudiante, instructor, franja.getId(), franja.getFecha(), "10:00", "11:00",
            1, AHORA.atZone(ZONA).toInstant());
        existente.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull(franja.getId(), estudiante,
            EstadoReserva.CONFIRMADA)).thenReturn(Optional.of(existente));
        when(pagosClient.saldo(BEARER)).thenReturn(4);

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.reservadas()).singleElement()
            .satisfies(r -> assertThat(r.codigo()).isEqualTo(existente.getCodigoReserva()));
        assertThat(resultado.rechazadas()).isEmpty();
        verify(reservaRepository, never()).save(any());
        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
    }

    // ---------------------------------------------------------------- petición inválida

    @Test
    void reservar_sinFranjasConMasDelMaximoORepetidas_lanzaErrSys01() {
        List<UUID> once = IntStream.range(0, 11).mapToObj(i -> UUID.randomUUID()).toList();
        UUID repetida = UUID.randomUUID();

        assertError(() -> service.reservar(estudiante, escuela, BEARER, List.of()), "ERR_SYS_01");
        assertError(() -> service.reservar(estudiante, escuela, BEARER, null), "ERR_SYS_01");
        assertError(() -> service.reservar(estudiante, escuela, BEARER, once), "ERR_SYS_01");
        assertError(() -> service.reservar(estudiante, escuela, BEARER, List.of(repetida, repetida)), "ERR_SYS_01");
        verifyNoInteractions(reservaRepository, franjaRepository, pagosClient);
    }

    @Test
    void reservar_conExactamenteElMaximo_loAcepta() {
        List<UUID> diez = IntStream.range(0, 10).mapToObj(i -> UUID.randomUUID()).toList();
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(any(), eq(escuela)))
            .thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(10);

        assertThat(service.reservar(estudiante, escuela, BEARER, diez).rechazadas()).hasSize(10);
    }

    @Test
    void reservar_siIdentidadNoResponde_laReservaSigueEnPieYElInstructorSeMuestraGenerico() {
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));
        when(reservaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(pagosClient.saldo(BEARER)).thenReturn(4);
        when(identidadClient.nombresDeInstructores(any(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        ReservasResultadoResponse resultado = reservar(franja);

        assertThat(resultado.reservadas()).singleElement()
            .satisfies(r -> assertThat(r.instructorNombre()).isEqualTo("Instructor"));
        verify(franjaRepository, never()).liberar(any(), any());
    }

    // ---------------------------------------------------------------- listar (mis clases e historial)

    private Reserva reservaDe(LocalDate fecha, String inicio, String fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(estudiante, instructor, 1L, fecha, inicio, fin, 1,
            AHORA.atZone(ZONA).toInstant());
        reserva.inicializarCodigo();
        reserva.setEstado(estado);
        return reserva;
    }

    @Test
    void listar_proximas_pideLasProximasConHoyYAhora_yUneElNombreDelInstructor() {
        Reserva reserva = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.CONFIRMADA);
        when(reservaRepository.proximas(eq(estudiante), eq(HOY), eq("09:30"), any()))
            .thenReturn(new PageImpl<>(List.of(reserva), PageRequest.of(0, 20), 1));

        Page<ReservaResponse> pagina = service.listar(estudiante, BEARER, "proximas", 1, 20);

        assertThat(pagina.getContent()).singleElement().satisfies(r -> {
            assertThat(r.codigo()).isEqualTo(reserva.getCodigoReserva());
            assertThat(r.instructorNombre()).isEqualTo("Ivan Alfa");
            assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(r.terminada()).isFalse();
        });
        verify(reservaRepository, never()).historial(any(), any(), any(), any());
    }

    @Test
    void listar_sinVista_esProximas() {
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(estudiante, BEARER, null, 1, 20);

        verify(reservaRepository).proximas(eq(estudiante), any(), any(), any());
    }

    @Test
    void listar_historial_marcaTerminadaLaConfirmadaQueYaPaso_yNoLaQueSigueEnCurso() {
        Reserva pasada = reservaDe(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        Reserva ayer = reservaDe(HOY.minusDays(1), "23:00", "23:59", EstadoReserva.CONFIRMADA);
        Reserva cancelada = reservaDe(HOY.plusDays(2), "10:00", "11:00", EstadoReserva.CANCELADA);
        Reserva justoAhora = reservaDe(HOY, "08:30", "09:30", EstadoReserva.CONFIRMADA);
        Reserva enCurso = reservaDe(HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA);
        when(reservaRepository.historial(eq(estudiante), eq(HOY), eq("09:30"), any()))
            .thenReturn(new PageImpl<>(List.of(pasada, ayer, cancelada, justoAhora, enCurso),
                PageRequest.of(0, 20), 5));

        Page<ReservaResponse> pagina = service.listar(estudiante, BEARER, "historial", 1, 20);

        assertThat(pagina.getContent()).extracting(ReservaResponse::terminada)
            .containsExactly(true, true, false, true, false);
        assertThat(pagina.getContent().get(2).estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void listar_normalizaLaPagina() {
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(estudiante, BEARER, "proximas", 0, 500);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaRepository).proximas(any(), any(), any(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isZero();
        assertThat(pagina.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listar_sinReservas_noLlamaAIdentidad() {
        when(reservaRepository.historial(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(service.listar(estudiante, BEARER, "historial", 1, 20).getContent()).isEmpty();
        verifyNoInteractions(identidadClient);
    }

    @Test
    void listar_conUnaVistaInvalida_lanzaErrSys01SinConsultar() {
        assertError(() -> service.listar(estudiante, BEARER, "futuras", 1, 20), "ERR_SYS_01");
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void listar_siIdentidadNoResponde_fallaConErrAca17() {
        Reserva reserva = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.CONFIRMADA);
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(reserva), PageRequest.of(0, 20), 1));
        when(identidadClient.nombresDeInstructores(any(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        assertError(() -> service.listar(estudiante, BEARER, "proximas", 1, 20), "ERR_ACA_17");
    }

    // ---------------------------------------------------------------- retomar reservas a medias

    /** Una reserva `PENDIENTE_PAGO` con su franja todavía apartada por ella. */
    private Reserva pendienteConFranjaApartada() {
        Franja franja = franja(HOY.plusDays(1), "10:00", "11:00");
        Reserva reserva = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.PENDIENTE_PAGO);
        reserva.setId(secuencia.incrementAndGet());
        franja.setEstado(EstadoFranja.RESERVADA);
        franja.setReservaId(reserva.getId());
        when(franjaRepository.findById(reserva.getFranjaId())).thenReturn(Optional.of(franja));
        // * lenient: un test que retoma varias pendientes vuelve a definir esta lista
        lenient().when(reservaRepository.pendientesDeEstudiante(eq(estudiante), any()))
            .thenReturn(List.of(reserva));
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        return reserva;
    }

    @Test
    void listar_retomaLasPendientesDeMasDeDosMinutos() {
        pendienteConFranjaApartada();

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(reservaRepository).pendientesDeEstudiante(eq(estudiante), limite.capture());
        assertThat(limite.getValue()).isEqualTo(AHORA.atZone(ZONA).toInstant().minusSeconds(120));
    }

    @Test
    void listar_unaPendienteConElDescuentoYaHecho_laConfirmaSinCobrarDeNuevo_conLaMismaClave() {
        Reserva pendiente = pendienteConFranjaApartada();
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 4));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(pagosClient).descontar(BEARER, estudiante, instructor, pendiente.getCodigoReserva(), 1,
            "reserva-" + pendiente.getCodigoReserva());
        verify(reservaRepository).cambiarEstado(eq(pendiente.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CONFIRMADA), anyString(), any());
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void listar_unaPendienteSinHoras_liberaLaFranjaYLaCancela() {
        Reserva pendiente = pendienteConFranjaApartada();
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new SinHorasException());

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(franjaRepository).liberar(any(UUID.class), eq(pendiente.getId()));
        verify(reservaRepository).cambiarEstado(eq(pendiente.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CANCELADA), eq("Sin horas suficientes"), any());
    }

    @Test
    void listar_siPagosNoResponde_laPendienteSigueIgualYElListadoNoFalla() {
        Reserva pendiente = pendienteConFranjaApartada();
        when(pagosClient.descontar(any(), any(), any(), any(), anyInt(), anyString()))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        assertThat(service.listar(estudiante, BEARER, "proximas", 1, 20).getContent()).isEmpty();

        verify(reservaRepository, never()).cambiarEstado(eq(pendiente.getCodigoReserva()), any(), any(), any(), any(),
            any());
        verify(franjaRepository, never()).liberar(any(), any());
    }

    @Test
    void listar_unaPendienteCuyaFranjaYaNoEstabaApartada_devuelveLaHoraYLaCancela() {
        Reserva pendiente = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.PENDIENTE_PAGO);
        pendiente.setId(secuencia.incrementAndGet());
        when(franjaRepository.findById(pendiente.getFranjaId())).thenReturn(Optional.empty());
        when(reservaRepository.pendientesDeEstudiante(eq(estudiante), any())).thenReturn(List.of(pendiente));
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(pagosClient).devolver(BEARER, estudiante, pendiente.getCodigoReserva(),
            "devolucion-" + pendiente.getCodigoReserva());
        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(reservaRepository).cambiarEstado(eq(pendiente.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CANCELADA), anyString(), any());
    }

    @Test
    void listar_unFalloRetomandoUnaNoImpideRetomarLasDemas() {
        Reserva primera = pendienteConFranjaApartada();
        Reserva segunda = new Reserva(estudiante, instructor, 2L, HOY.plusDays(2), "12:00", "13:00", 1,
            AHORA.atZone(ZONA).toInstant());
        segunda.inicializarCodigo();
        segunda.setId(secuencia.incrementAndGet());
        Franja franjaSegunda = franja(HOY.plusDays(2), "12:00", "13:00");
        franjaSegunda.setEstado(EstadoFranja.RESERVADA);
        franjaSegunda.setReservaId(segunda.getId());
        when(franjaRepository.findById(2L)).thenReturn(Optional.of(franjaSegunda));
        when(reservaRepository.pendientesDeEstudiante(eq(estudiante), any())).thenReturn(List.of(primera, segunda));
        when(pagosClient.descontar(any(), any(), any(), eq(primera.getCodigoReserva()), anyInt(), anyString()))
            .thenThrow(new IllegalStateException("falla inesperada"));
        when(pagosClient.descontar(any(), any(), any(), eq(segunda.getCodigoReserva()), anyInt(), anyString()))
            .thenReturn(new ResultadoConsumo(1, 3));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(reservaRepository).cambiarEstado(eq(segunda.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CONFIRMADA), anyString(), any());
    }

    @Test
    void reservar_sinEscuelaEnElToken_lanzaErrAca16() {
        assertError(() -> service.reservar(estudiante, null, BEARER, List.of(UUID.randomUUID())), "ERR_ACA_16");
        verifyNoInteractions(reservaRepository, franjaRepository, pagosClient);
    }
}
