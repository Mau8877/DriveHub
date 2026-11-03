package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.horarios.dto.ResumenFranjasInstructorResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;

/**
 * El resumen de franjas por instructor (CU11). La autorización por rol NO se prueba acá (un servicio sin
 * Spring no ejecuta @PreAuthorize).
 */
@ExtendWith(MockitoExtension.class)
class FranjaResumenServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();

    @Mock
    private FranjaRepository franjaRepository;

    private final UUID escuela = UUID.randomUUID();
    private final UUID instructorA = UUID.randomUUID();
    private final UUID instructorB = UUID.randomUUID();
    private FranjaResumenService service;

    @BeforeEach
    void setUp() {
        service = new FranjaResumenService(franjaRepository, Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
    }

    private Franja franja(UUID instructor, LocalDate fecha, String inicio, EstadoFranja estado) {
        Franja franja = new Franja(escuela, 1L, instructor, fecha, inicio, "23:59");
        franja.setEstado(estado);
        return franja;
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void resumir_cuentaLasFranjasDeCadaInstructorPorEstado() {
        LocalDate manana = HOY.plusDays(1);
        when(franjaRepository.delDia(escuela, manana, null, null)).thenReturn(List.of(
            franja(instructorA, manana, "08:00", EstadoFranja.LIBRE),
            franja(instructorA, manana, "09:00", EstadoFranja.LIBRE),
            franja(instructorA, manana, "10:00", EstadoFranja.RESERVADA),
            franja(instructorA, manana, "11:00", EstadoFranja.BLOQUEADA),
            franja(instructorB, manana, "08:00", EstadoFranja.RESERVADA)));

        List<ResumenFranjasInstructorResponse> resumen = service.resumirDelDia(escuela, manana, null, null);

        assertThat(resumen).hasSize(2);
        ResumenFranjasInstructorResponse a = resumen.stream()
            .filter(r -> r.instructorCodigo().equals(instructorA)).findFirst().orElseThrow();
        assertThat(a.total()).isEqualTo(4);
        assertThat(a.libres()).isEqualTo(2);
        assertThat(a.reservadas()).isEqualTo(1);
        assertThat(a.bloqueadas()).isEqualTo(1);
        ResumenFranjasInstructorResponse b = resumen.stream()
            .filter(r -> r.instructorCodigo().equals(instructorB)).findFirst().orElseThrow();
        assertThat(b.total()).isEqualTo(1);
        assertThat(b.libres()).isZero();
        assertThat(b.reservadas()).isEqualTo(1);
    }

    @Test
    void resumir_hoy_noCuentaComoLibresLasQueYaEmpezaron() {
        when(franjaRepository.delDia(escuela, HOY, null, null)).thenReturn(List.of(
            franja(instructorA, HOY, "08:00", EstadoFranja.LIBRE),
            franja(instructorA, HOY, "09:30", EstadoFranja.LIBRE),
            franja(instructorA, HOY, "09:31", EstadoFranja.LIBRE),
            franja(instructorA, HOY, "15:00", EstadoFranja.LIBRE)));

        ResumenFranjasInstructorResponse a = service.resumirDelDia(escuela, null, null, null).get(0);

        // * Las de las 08:00 y las 09:30 ya empezaron; siguen en el total pero no son libres
        assertThat(a.total()).isEqualTo(4);
        assertThat(a.libres()).isEqualTo(2);
    }

    @Test
    void resumir_unInstructorSoloConFranjasPasadas_apareceConTotalYSinLibres() {
        when(franjaRepository.delDia(escuela, HOY, null, null))
            .thenReturn(List.of(franja(instructorA, HOY, "07:00", EstadoFranja.LIBRE)));

        ResumenFranjasInstructorResponse a = service.resumirDelDia(escuela, HOY, null, null).get(0);

        assertThat(a.total()).isEqualTo(1);
        assertThat(a.libres()).isZero();
    }

    @Test
    void resumir_sinFecha_usaHoy() {
        when(franjaRepository.delDia(escuela, HOY, null, null)).thenReturn(List.of());

        assertThat(service.resumirDelDia(escuela, null, null, null)).isEmpty();
        verify(franjaRepository).delDia(escuela, HOY, null, null);
    }

    @Test
    void resumir_pasaLaVentanaDeHorasAlRepositorio() {
        when(franjaRepository.delDia(escuela, HOY.plusDays(2), "12:00", "18:00")).thenReturn(List.of());

        service.resumirDelDia(escuela, HOY.plusDays(2), "12:00", "18:00");

        verify(franjaRepository).delDia(escuela, HOY.plusDays(2), "12:00", "18:00");
    }

    @Test
    void resumir_losInstructoresVienenOrdenadosPorCodigo() {
        LocalDate manana = HOY.plusDays(1);
        when(franjaRepository.delDia(escuela, manana, null, null)).thenReturn(List.of(
            franja(instructorB, manana, "08:00", EstadoFranja.LIBRE),
            franja(instructorA, manana, "08:00", EstadoFranja.LIBRE)));

        List<UUID> codigos = service.resumirDelDia(escuela, manana, null, null).stream()
            .map(ResumenFranjasInstructorResponse::instructorCodigo).toList();

        assertThat(codigos).isSorted();
    }

    @Test
    void resumir_sinEscuelaEnElToken_fallaConErrAca16() {
        assertError(() -> service.resumirDelDia(null, HOY, null, null), "ERR_ACA_16");

        verify(franjaRepository, never()).delDia(any(), any(), any(), any());
    }

    @Test
    void resumir_conHoraInvalida_fallaConErrSys01() {
        assertError(() -> service.resumirDelDia(escuela, HOY, "25:00", null), "ERR_SYS_01");
        assertError(() -> service.resumirDelDia(escuela, HOY, null, "9:00"), "ERR_SYS_01");

        verify(franjaRepository, never()).delDia(any(), any(), any(), any());
    }

    @Test
    void resumir_conLaHoraDeInicioNoAnteriorALaDeFin_fallaConErrSys01() {
        assertError(() -> service.resumirDelDia(escuela, HOY, "18:00", "12:00"), "ERR_SYS_01");
        assertError(() -> service.resumirDelDia(escuela, HOY, "12:00", "12:00"), "ERR_SYS_01");
    }
}
