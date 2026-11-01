package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.OrquestadorProperties;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.client.ClientesDePagos;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.client.PagosClient;
import com.drivehub.academico.features.reservas.client.PagosClient.ResultadoConsumo;
import com.drivehub.academico.features.reservas.client.SinHorasException;
import com.drivehub.academico.features.reservas.dto.FranjaRechazadaResponse;
import com.drivehub.academico.features.reservas.dto.CancelacionResponse;
import com.drivehub.academico.features.reservas.dto.HoraDeCancelacion;
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
    @Mock
    private ClienteDePagos clienteDeServicio;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final AtomicLong secuencia = new AtomicLong(100);
    private ReservaService service;
    private OrquestadorClases orquestador;
    private PlazoDeClase plazo;
    private ManejadorDeReprogramacion reprogramaciones;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        // * La saga real (CU44) sobre repositorios y Pagos simulados: reservar y retomar usan el mismo código que el job
        OrquestadorProperties orquestadorProps = new OrquestadorProperties(60, 5, true, "0 * * * * *", 50);
        CompensadorDeReserva compensador = new CompensadorDeReserva(reservaRepository, franjaRepository, reloj);
        ManejadorDeReserva manejador = new ManejadorDeReserva(reservaRepository, franjaRepository, compensador,
            orquestadorProps, reloj);
        reprogramaciones = new ManejadorDeReprogramacion(reservaRepository, franjaRepository, reloj);
        orquestador = new OrquestadorClases(List.of(manejador, new ManejadorDeCancelacion(compensador),
            reprogramaciones), compensador, reservaRepository, clienteDeServicio, orquestadorProps, reloj);
        ReservasProperties propiedades = new ReservasProperties(1, 10, 2, 12);
        plazo = new PlazoDeClase(propiedades, reloj);
        service = new ReservaService(reservaRepository, franjaRepository, pagosClient,
            new ClientesDePagos(pagosClient), orquestador, identidadClient, plazo, reprogramaciones, propiedades,
            reloj);
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
        // * lo que Mongo llena al guardar: el orquestador decide completar o deshacer por esta edad
        ReflectionTestUtils.setField(reserva, "createdAt", AHORA.atZone(ZONA).toInstant());
        return reserva;
    }

    /** El orquestador vuelve a leer la reserva antes de actuar. */
    private void laReservaSeVuelveALeer(Reserva reserva) {
        lenient().when(reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(
            reserva.getCodigoReserva(), estudiante)).thenReturn(Optional.of(reserva));
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
        laReservaSeVuelveALeer(reserva);
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
        // * la franja existe pero volvió a estar libre
        Franja libre = franja(HOY.plusDays(1), "10:00", "11:00");
        when(franjaRepository.findById(pendiente.getFranjaId())).thenReturn(Optional.of(libre));
        laReservaSeVuelveALeer(pendiente);
        when(reservaRepository.pendientesDeEstudiante(eq(estudiante), any())).thenReturn(List.of(pendiente));
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(pagosClient).devolver(BEARER, estudiante, pendiente.getCodigoReserva(),
            "devolucion-" + pendiente.getCodigoReserva());
        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository).cambiarEstado(eq(pendiente.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CANCELADA), anyString(), any());
    }

    @Test
    void listar_unaPendienteVieja_laDeshaceEnVezDeConfirmarla_igualQueElProcesoProgramado() {
        Reserva vieja = pendienteConFranjaApartada();
        ReflectionTestUtils.setField(vieja, "createdAt", AHORA.atZone(ZONA).toInstant().minusSeconds(60 * 60));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(pagosClient).devolver(BEARER, estudiante, vieja.getCodigoReserva(),
            "devolucion-" + vieja.getCodigoReserva());
        verify(franjaRepository).liberar(any(UUID.class), eq(vieja.getId()));
        verify(reservaRepository).cambiarEstado(eq(vieja.getCodigoReserva()), eq(estudiante),
            eq(EstadoReserva.PENDIENTE_PAGO), eq(EstadoReserva.CANCELADA), anyString(), any());
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), eq(EstadoReserva.CONFIRMADA), any(),
            any());
    }

    @Test
    void listar_unaPendienteSinFranja_noSeTocaYElListadoNoFalla() {
        Reserva pendiente = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.PENDIENTE_PAGO);
        pendiente.setId(secuencia.incrementAndGet());
        when(franjaRepository.findById(pendiente.getFranjaId())).thenReturn(Optional.empty());
        laReservaSeVuelveALeer(pendiente);
        when(reservaRepository.pendientesDeEstudiante(eq(estudiante), any())).thenReturn(List.of(pendiente));
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(estudiante, BEARER, "proximas", 1, 20);

        verifyNoInteractions(pagosClient);
        verify(reservaRepository, never()).cambiarEstado(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reservar_laSagaLaHaceElOrquestador_elServicioNoDescuentaNiDevuelveDirecto() {
        OrquestadorClases delegado = org.mockito.Mockito.mock(OrquestadorClases.class);
        ReservaService conOrquestadorFalso = new ReservaService(reservaRepository, franjaRepository, pagosClient,
            new ClientesDePagos(pagosClient), delegado, identidadClient, plazo, reprogramaciones,
            new ReservasProperties(1, 10, 2, 12), Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
        Franja franja = franjaReservable("10:00");
        seGuardanLasReservasConId();
        laFranjaSePuedeApartar(franja);
        when(delegado.completarReserva(any(Reserva.class), eq(franja), any(ClienteDePagos.class)))
            .thenAnswer(inv -> new OrquestadorClases.ReservaCompletada(
                OrquestadorClases.ResultadoReserva.CONFIRMADA, inv.getArgument(0)));
        when(pagosClient.saldo(BEARER)).thenReturn(4);

        ReservasResultadoResponse resultado = conOrquestadorFalso.reservar(estudiante, escuela, BEARER,
            List.of(franja.getCodigoFranja()));

        assertThat(resultado.reservadas()).hasSize(1);
        verify(delegado).completarReserva(any(Reserva.class), eq(franja), any(ClienteDePagos.class));
        verify(pagosClient, never()).descontar(any(), any(), any(), any(), anyInt(), anyString());
        verify(pagosClient, never()).devolver(any(), any(), any(), anyString());
    }

    @Test
    void listar_unFalloRetomandoUnaNoImpideRetomarLasDemas() {
        Reserva primera = pendienteConFranjaApartada();
        Reserva segunda = new Reserva(estudiante, instructor, 2L, HOY.plusDays(2), "12:00", "13:00", 1,
            AHORA.atZone(ZONA).toInstant());
        segunda.inicializarCodigo();
        segunda.setId(secuencia.incrementAndGet());
        ReflectionTestUtils.setField(segunda, "createdAt", AHORA.atZone(ZONA).toInstant());
        laReservaSeVuelveALeer(segunda);
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

    // ------------------------------------------------------------------ cancelar (CU15)

    /** Una clase confirmada del estudiante, en la franja `RESERVADA` por ella. */
    private Reserva claseConfirmada(LocalDate fecha, String inicio, String fin) {
        Reserva reserva = reservaDe(fecha, inicio, fin, EstadoReserva.CONFIRMADA);
        reserva.setId(secuencia.incrementAndGet());
        laReservaSeVuelveALeer(reserva);
        return reserva;
    }

    private Franja franjaApartadaPor(Reserva reserva) {
        Franja franja = franja(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        franja.setEstado(EstadoFranja.RESERVADA);
        franja.setReservaId(reserva.getId());
        lenient().when(franjaRepository.findById(reserva.getFranjaId())).thenReturn(Optional.of(franja));
        return franja;
    }

    private void laCancelacionSeInicia(Reserva reserva) {
        when(reservaRepository.iniciarCancelacion(eq(reserva.getCodigoReserva()), eq(estudiante), anyBoolean(),
            anyString(), any())).thenAnswer(inv -> {
                reserva.setEstado(EstadoReserva.CANCELACION_PENDIENTE);
                ReflectionTestUtils.setField(reserva, "sinDevolucion", inv.getArgument(2));
                return Optional.of(reserva);
            });
    }

    private static void falla(Runnable accion, Class<? extends BusinessException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).hasFieldOrPropertyWithValue("errorCode", codigo);
    }

    @Test
    void cancelar_aTiempo_devuelveLaHora_liberaLaFranja_yLaClaseQuedaCancelada() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja franja = franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(pagosClient.saldo(BEARER)).thenReturn(5);

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
        assertThat(respuesta.horasDisponibles()).isEqualTo(5);
        assertThat(respuesta.reserva().estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(respuesta.reserva().instructorNombre()).isEqualTo("Ivan Alfa");
        verify(reservaRepository).iniciarCancelacion(eq(reserva.getCodigoReserva()), eq(estudiante), eq(false),
            anyString(), any());
        verify(pagosClient).devolver(BEARER, estudiante, reserva.getCodigoReserva(),
            "devolucion-" + reserva.getCodigoReserva());
        verify(franjaRepository).liberar(franja.getCodigoFranja(), reserva.getId());
    }

    @Test
    void cancelar_tarde_liberaLaFranja_peroLaHoraSePierdeYNoSeLlamaAPagos() {
        Reserva reserva = claseConfirmada(HOY, "15:00", "16:00");
        Franja franja = franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(pagosClient.saldo(BEARER)).thenReturn(3);

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.PERDIDA);
        assertThat(respuesta.horasDisponibles()).isEqualTo(3);
        assertThat(respuesta.reserva().estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(reservaRepository).iniciarCancelacion(eq(reserva.getCodigoReserva()), eq(estudiante), eq(true),
            anyString(), any());
        verify(pagosClient, never()).devolver(any(), any(), any(), anyString());
        verify(franjaRepository).liberar(franja.getCodigoFranja(), reserva.getId());
    }

    @Test
    void cancelar_justoEnElLimiteDeDoceHoras_cuentaComoATiempo() {
        // * ahora 09:30, la clase es a las 21:30 = exactamente 12 h
        Reserva reserva = claseConfirmada(HOY, "21:30", "22:30");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(pagosClient.saldo(BEARER)).thenReturn(1);

        assertThat(service.cancelar(estudiante, BEARER, reserva.getCodigoReserva()).horaDevuelta())
            .isEqualTo(HoraDeCancelacion.DEVUELTA);
    }

    @Test
    void cancelar_pagosNoResponde_laCancelacionSeAcepta_yLaHoraQuedaEnProceso() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        doThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No")).when(pagosClient)
            .devolver(any(), any(), any(), anyString());

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.EN_PROCESO);
        assertThat(respuesta.horasDisponibles()).isNull();
        // * sigue CANCELACION_PENDIENTE (el cliente la ve cancelada) y la franja no se liberó todavía
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(respuesta.reserva().estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(franjaRepository, never()).liberar(any(), any());
        verify(pagosClient, never()).saldo(any());
    }

    @Test
    void cancelar_unFalloInesperadoAlTerminar_noDeshaceLaCancelacion() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        laCancelacionSeInicia(reserva);
        when(franjaRepository.findById(reserva.getFranjaId())).thenThrow(new IllegalStateException("Mongo caído"));

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.EN_PROCESO);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
    }

    @Test
    void cancelar_siLaLecturaDelSaldoFalla_lasHorasDisponiblesVienenNulas_yNadaSeDeshace() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(pagosClient.saldo(BEARER)).thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
        assertThat(respuesta.horasDisponibles()).isNull();
    }

    @Test
    void cancelar_siIdentidadNoResponde_elNombreSeMuestraComoInstructor() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        laCancelacionSeInicia(reserva);
        when(pagosClient.saldo(BEARER)).thenReturn(2);
        when(identidadClient.nombresDeInstructores(any(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        CancelacionResponse respuesta = service.cancelar(estudiante, BEARER, reserva.getCodigoReserva());

        assertThat(respuesta.reserva().instructorNombre()).isEqualTo("Instructor");
        assertThat(respuesta.horaDevuelta()).isEqualTo(HoraDeCancelacion.DEVUELTA);
    }

    @Test
    void cancelar_unaReservaQueNoExisteOEsDeOtro_esERR_ACA_32() {
        UUID codigo = UUID.randomUUID();
        when(reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(codigo, estudiante))
            .thenReturn(Optional.empty());

        falla(() -> service.cancelar(estudiante, BEARER, codigo), BusinessException.class,
            AcademicoErrorCodes.RESERVA_NO_ENCONTRADA);

        verify(reservaRepository, never()).iniciarCancelacion(any(), any(), anyBoolean(), anyString(), any());
    }

    @Test
    void cancelar_unaReservaQueNoEstaConfirmada_esERR_ACA_33_ySinCambios() {
        for (EstadoReserva estado : List.of(EstadoReserva.CANCELADA, EstadoReserva.COMPLETADA,
                EstadoReserva.NO_ASISTIO, EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELACION_PENDIENTE,
                EstadoReserva.REPROGRAMACION_PENDIENTE)) {
            Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
            reserva.setEstado(estado);

            falla(() -> service.cancelar(estudiante, BEARER, reserva.getCodigoReserva()), BusinessException.class,
                AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);
        }
        verify(reservaRepository, never()).iniciarCancelacion(any(), any(), anyBoolean(), anyString(), any());
        verifyNoInteractions(pagosClient);
    }

    @Test
    void cancelar_unaClaseQueYaEmpezo_esERR_ACA_33() {
        Reserva reserva = claseConfirmada(HOY, "08:00", "09:00");

        falla(() -> service.cancelar(estudiante, BEARER, reserva.getCodigoReserva()), BusinessException.class,
            AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);

        verify(reservaRepository, never()).iniciarCancelacion(any(), any(), anyBoolean(), anyString(), any());
    }

    @Test
    void cancelar_siOtraPeticionLaCancelaPrimero_laSegundaRecibeERR_ACA_33() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        when(reservaRepository.iniciarCancelacion(eq(reserva.getCodigoReserva()), eq(estudiante), anyBoolean(),
            anyString(), any())).thenReturn(Optional.empty());

        falla(() -> service.cancelar(estudiante, BEARER, reserva.getCodigoReserva()), BusinessException.class,
            AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);

        verifyNoInteractions(pagosClient);
    }

    // ------------------------------------------------------------------ reprogramar (CU15)

    /** Las operaciones atómicas del repositorio, con el efecto que tendrían en Mongo. */
    private void laReprogramacionSePuedeHacer(Reserva reserva, Franja destino) {
        lenient().when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(
            destino.getCodigoFranja(), escuela)).thenReturn(Optional.of(destino));
        lenient().when(franjaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        lenient().when(reservaRepository.existeSolape(estudiante, destino.getFecha(), destino.getHoraInicio(),
            reserva.getCodigoReserva())).thenReturn(false);
        lenient().when(reservaRepository.iniciarReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante),
            eq(destino.getId()), anyString(), any())).thenAnswer(inv -> {
                reserva.setEstado(EstadoReserva.REPROGRAMACION_PENDIENTE);
                ReflectionTestUtils.setField(reserva, "franjaDestinoId", destino.getId());
                return Optional.of(reserva);
            });
        lenient().when(franjaRepository.apartar(eq(destino.getCodigoFranja()), eq(escuela), eq(reserva.getId()),
            any(), anyString())).thenAnswer(inv -> {
                destino.setEstado(EstadoFranja.RESERVADA);
                destino.setReservaId(reserva.getId());
                return Optional.of(destino);
            });
        lenient().when(reservaRepository.completarReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante),
            eq(destino.getId()), any(), anyString(), anyString(), any(), any())).thenAnswer(inv -> {
                reserva.setEstado(EstadoReserva.CONFIRMADA);
                ReflectionTestUtils.setField(reserva, "franjaId", destino.getId());
                ReflectionTestUtils.setField(reserva, "fecha", destino.getFecha());
                ReflectionTestUtils.setField(reserva, "horaInicio", destino.getHoraInicio());
                ReflectionTestUtils.setField(reserva, "horaFin", destino.getHoraFin());
                ReflectionTestUtils.setField(reserva, "franjaDestinoId", null);
                return Optional.of(reserva);
            });
    }

    private Franja franjaDestino(LocalDate fecha, String inicio, String fin) {
        Franja destino = franja(fecha, inicio, fin);
        destino.setEstado(EstadoFranja.LIBRE);
        return destino;
    }

    @Test
    void reprogramar_mueveLaClaseALaFranjaNueva_liberaLaAnterior_yNoTocaPagos() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja anterior = franjaApartadaPor(reserva);
        Franja destino = franjaDestino(HOY.plusDays(2), "15:00", "16:00");
        laReprogramacionSePuedeHacer(reserva, destino);

        ReservaResponse respuesta = service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja());

        assertThat(respuesta.codigo()).isEqualTo(reserva.getCodigoReserva());
        assertThat(respuesta.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(respuesta.fecha()).isEqualTo(HOY.plusDays(2));
        assertThat(respuesta.horaInicio()).isEqualTo("15:00");
        assertThat(respuesta.cancelacionATiempo()).isTrue();
        verify(franjaRepository).liberar(anterior.getCodigoFranja(), reserva.getId());
        verifyNoInteractions(pagosClient);
    }

    @Test
    void reprogramar_sinLimiteDeVeces_unaClaseSePuedeMoverDosVeces() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        Franja primera = franjaDestino(HOY.plusDays(2), "15:00", "16:00");
        Franja segunda = franjaDestino(HOY.plusDays(3), "09:00", "10:00");
        laReprogramacionSePuedeHacer(reserva, primera);
        laReprogramacionSePuedeHacer(reserva, segunda);
        lenient().when(franjaRepository.findById(primera.getId())).thenReturn(Optional.of(primera));

        service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(), primera.getCodigoFranja());
        ReservaResponse segunda2 = service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            segunda.getCodigoFranja());

        assertThat(segunda2.fecha()).isEqualTo(HOY.plusDays(3));
        assertThat(segunda2.horaInicio()).isEqualTo("09:00");
    }

    @Test
    void reprogramar_laFranjaYaNoEstaLibre_vuelveAtras_ylaClaseQuedaEnLaOriginal() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        franjaApartadaPor(reserva);
        Franja destino = franjaDestino(HOY.plusDays(2), "15:00", "16:00");
        laReprogramacionSePuedeHacer(reserva, destino);
        when(franjaRepository.apartar(eq(destino.getCodigoFranja()), eq(escuela), eq(reserva.getId()), any(),
            anyString())).thenReturn(Optional.empty());

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.FRANJA_NO_DISPONIBLE);

        verify(reservaRepository).revertirReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante), anyString(),
            any());
        verify(franjaRepository, never()).liberar(any(), any());
        verify(reservaRepository, never()).completarReprogramacion(any(), any(), any(), any(), any(), any(), any(),
            any());
    }

    @Test
    void reprogramar_conMenosDeDoceHoras_esERR_ACA_34_ySinCambios() {
        Reserva reserva = claseConfirmada(HOY, "15:00", "16:00");
        Franja destino = franjaDestino(HOY.plusDays(2), "15:00", "16:00");

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.RESERVA_FUERA_DE_PLAZO);

        verify(reservaRepository, never()).iniciarReprogramacion(any(), any(), any(), anyString(), any());
    }

    @Test
    void reprogramar_unaReservaAjena_esERR_ACA_32() {
        UUID codigo = UUID.randomUUID();
        when(reservaRepository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(codigo, estudiante))
            .thenReturn(Optional.empty());

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, codigo, UUID.randomUUID()),
            BusinessException.class, AcademicoErrorCodes.RESERVA_NO_ENCONTRADA);
    }

    @Test
    void reprogramar_unaReservaNoConfirmadaOYaEmpezada_esERR_ACA_33() {
        Reserva cancelada = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        cancelada.setEstado(EstadoReserva.CANCELADA);
        Reserva empezada = claseConfirmada(HOY, "08:00", "09:00");

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, cancelada.getCodigoReserva(), UUID.randomUUID()),
            BusinessException.class, AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);
        falla(() -> service.reprogramar(estudiante, escuela, BEARER, empezada.getCodigoReserva(), UUID.randomUUID()),
            BusinessException.class, AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);
    }

    @Test
    void reprogramar_aLaMismaFranjaEnLaQueYaEsta_esUnaValidacion() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja actual = franjaDestino(HOY.plusDays(1), "10:00", "11:00");
        actual.setId(reserva.getFranjaId());
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(actual.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(actual));

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            actual.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.VALIDACION);
    }

    @Test
    void reprogramar_aUnaFranjaQueNoExisteODeOtraEscuela_esERR_ACA_18() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        UUID desconocida = UUID.randomUUID();
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(desconocida, escuela))
            .thenReturn(Optional.empty());

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(), desconocida),
            BusinessException.class, AcademicoErrorCodes.FRANJA_NO_DISPONIBLE);
    }

    @Test
    void reprogramar_aUnaFranjaQueYaEmpezo_esERR_ACA_10() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja destino = franjaDestino(HOY, "09:00", "10:00");
        when(franjaRepository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(destino.getCodigoFranja(), escuela))
            .thenReturn(Optional.of(destino));

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.FRANJA_YA_EMPEZO);
    }

    @Test
    void reprogramar_aUnaHoraEnLaQueYaTieneOtraClase_esERR_ACA_20_yLaPropiaNoCuenta() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja destino = franjaDestino(HOY.plusDays(2), "15:00", "16:00");
        laReprogramacionSePuedeHacer(reserva, destino);
        when(reservaRepository.existeSolape(estudiante, destino.getFecha(), "15:00", reserva.getCodigoReserva()))
            .thenReturn(true);

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.CLASE_SOLAPADA);

        verify(reservaRepository, never()).iniciarReprogramacion(any(), any(), any(), anyString(), any());
    }

    @Test
    void reprogramar_sinEscuelaEnElToken_esERR_ACA_16() {
        falla(() -> service.reprogramar(estudiante, null, BEARER, UUID.randomUUID(), UUID.randomUUID()),
            BusinessException.class, AcademicoErrorCodes.TOKEN_SIN_ESCUELA);
    }

    @Test
    void reprogramar_siOtraPeticionCambioLaReserva_esERR_ACA_33() {
        Reserva reserva = claseConfirmada(HOY.plusDays(1), "10:00", "11:00");
        Franja destino = franjaDestino(HOY.plusDays(2), "15:00", "16:00");
        laReprogramacionSePuedeHacer(reserva, destino);
        when(reservaRepository.iniciarReprogramacion(eq(reserva.getCodigoReserva()), eq(estudiante),
            eq(destino.getId()), anyString(), any())).thenReturn(Optional.empty());

        falla(() -> service.reprogramar(estudiante, escuela, BEARER, reserva.getCodigoReserva(),
            destino.getCodigoFranja()), BusinessException.class, AcademicoErrorCodes.RESERVA_NO_MODIFICABLE);

        verify(franjaRepository, never()).apartar(any(), any(), any(), any(), anyString());
    }

    // ------------------------------------------------------------------ autorización (CU15)

    @Test
    void cancelarYReprogramar_soloLasPuedeHacerUnEstudiante() throws Exception {
        // * un servicio sin Spring no ejecuta @PreAuthorize: se comprueba que la regla esté declarada
        var cancelar = ReservaService.class.getMethod("cancelar", UUID.class, String.class, UUID.class);
        var reprogramar = ReservaService.class.getMethod("reprogramar", UUID.class, UUID.class, String.class,
            UUID.class, UUID.class);

        for (var metodo : List.of(cancelar, reprogramar)) {
            var regla = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            assertThat(regla).as(metodo.getName()).isNotNull();
            assertThat(regla.value()).isEqualTo("hasRole('ESTUDIANTE')");
        }
    }

    // ------------------------------------------------------------------ lo que ve el cliente (CU15)

    @Test
    void listar_lasReservasConfirmadasDicenSiTodaviaEstanATiempo() {
        Reserva lejana = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.CONFIRMADA);
        Reserva cercana = reservaDe(HOY, "15:00", "16:00", EstadoReserva.CONFIRMADA);
        Reserva yTerminada = reservaDe(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        when(reservaRepository.proximas(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(lejana, cercana, yTerminada), PageRequest.of(0, 20), 3));

        Page<ReservaResponse> pagina = service.listar(estudiante, BEARER, "proximas", 1, 20);

        assertThat(pagina.getContent()).extracting(ReservaResponse::cancelacionATiempo)
            .containsExactly(true, false, false);
    }

    @Test
    void listar_unaReservaCerradaNuncaEstaATiempo() {
        Reserva cancelada = reservaDe(HOY.plusDays(1), "10:00", "11:00", EstadoReserva.CANCELADA);
        when(reservaRepository.historial(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(cancelada), PageRequest.of(0, 20), 1));

        Page<ReservaResponse> pagina = service.listar(estudiante, BEARER, "historial", 1, 20);

        assertThat(pagina.getContent()).singleElement().satisfies(r -> assertThat(r.cancelacionATiempo()).isFalse());
    }
}
