package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class CierreDeReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final Instant INSTANTE = AHORA.atZone(ZONA).toInstant();

    @Mock
    private ReservaRepository reservaRepository;

    private final UUID instructor = UUID.randomUUID();
    private CierreDeReservaService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(INSTANTE, ZONA);
        PlazoDeClase plazo = new PlazoDeClase(new ReservasProperties(1, 10, 2, 12), reloj);
        service = new CierreDeReservaService(reservaRepository, plazo, reloj);
    }

    private Reserva reserva(LocalDate fecha, String inicio, String fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(UUID.randomUUID(), instructor, 7L, fecha, inicio, fin, 1, INSTANTE);
        reserva.inicializarCodigo();
        reserva.setEstado(estado);
        return reserva;
    }

    private void laReservaExiste(Reserva reserva) {
        when(reservaRepository.findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(reserva.getCodigoReserva(),
            instructor)).thenReturn(Optional.of(reserva));
    }

    private static void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---------------------------------------------------------------- buscarCerrable

    @Test
    void buscarCerrable_confirmadaQueYaEmpezo_laDevuelve() {
        Reserva reserva = reserva(HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        laReservaExiste(reserva);

        assertThat(service.buscarCerrable(instructor, reserva.getCodigoReserva())).isSameAs(reserva);
    }

    @Test
    void buscarCerrable_puedeCerrarseAntesDeLaHoraDeFin() {
        // * son las 09:30: la clase de 09:00 a 10:00 ya empezó y todavía no terminó
        Reserva reserva = reserva(HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA);
        laReservaExiste(reserva);

        assertThat(service.buscarCerrable(instructor, reserva.getCodigoReserva())).isSameAs(reserva);
    }

    @Test
    void buscarCerrable_inexistenteDeOtroInstructorOBorrada_lanzaNoEncontrada() {
        UUID codigo = UUID.randomUUID();
        when(reservaRepository.findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull(codigo, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.buscarCerrable(instructor, codigo), AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA);
    }

    @Test
    void buscarCerrable_siNoEstaConfirmada_lanzaNoCerrable() {
        for (EstadoReserva estado : EstadoReserva.values()) {
            if (estado == EstadoReserva.CONFIRMADA) {
                continue;
            }
            Reserva reserva = reserva(HOY, "08:00", "09:00", estado);
            laReservaExiste(reserva);

            assertError(() -> service.buscarCerrable(instructor, reserva.getCodigoReserva()),
                AcademicoErrorCodes.CLASE_NO_CERRABLE);
        }
    }

    @Test
    void buscarCerrable_confirmadaQueTodaviaNoEmpezo_lanzaNoCerrable() {
        Reserva reserva = reserva(HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA);
        laReservaExiste(reserva);

        assertError(() -> service.buscarCerrable(instructor, reserva.getCodigoReserva()),
            AcademicoErrorCodes.CLASE_NO_CERRABLE);
    }

    @Test
    void buscarCerrable_confirmadaDeAyer_sePuedeCerrar() {
        Reserva reserva = reserva(HOY.minusDays(1), "15:00", "16:00", EstadoReserva.CONFIRMADA);
        laReservaExiste(reserva);

        assertThat(service.buscarCerrable(instructor, reserva.getCodigoReserva())).isSameAs(reserva);
    }

    // ---------------------------------------------------------------- marcarCerrada

    @Test
    void marcarCerrada_presenteYTardeDejanCompletada() {
        UUID codigo = UUID.randomUUID();
        Reserva cerrada = reserva(HOY, "08:00", "09:00", EstadoReserva.COMPLETADA);
        when(reservaRepository.cerrar(any(), any(), any(), any(), any())).thenReturn(Optional.of(cerrada));

        assertThat(service.marcarCerrada(instructor, codigo, Asistencia.PRESENTE)).containsSame(cerrada);
        assertThat(service.marcarCerrada(instructor, codigo, Asistencia.TARDE)).containsSame(cerrada);

        verify(reservaRepository).cerrar(codigo, instructor, EstadoReserva.COMPLETADA,
            "Clase cerrada por el instructor (PRESENTE)", INSTANTE);
        verify(reservaRepository).cerrar(codigo, instructor, EstadoReserva.COMPLETADA,
            "Clase cerrada por el instructor (TARDE)", INSTANTE);
    }

    @Test
    void marcarCerrada_ausenteDejaNoAsistio() {
        UUID codigo = UUID.randomUUID();
        when(reservaRepository.cerrar(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.marcarCerrada(instructor, codigo, Asistencia.AUSENTE)).isEmpty();

        verify(reservaRepository).cerrar(codigo, instructor, EstadoReserva.NO_ASISTIO,
            "Clase cerrada por el instructor (AUSENTE)", INSTANTE);
        verifyNoMoreInteractions(reservaRepository);
    }
}
