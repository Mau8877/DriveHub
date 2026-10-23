package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.horarios.dto.DiaBloqueadoResponse;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;

@ExtendWith(MockitoExtension.class)
class FranjaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();

    @Mock
    private FranjaRepository franjaRepository;

    private final UUID instructor = UUID.randomUUID();
    private FranjaService service;

    @BeforeEach
    void setUp() {
        service = new FranjaService(franjaRepository, Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
    }

    private Franja franja(LocalDate fecha, String horaInicio, EstadoFranja estado) {
        Franja franja = new Franja(UUID.randomUUID(), 1L, instructor, fecha, horaInicio, "23:59");
        franja.inicializarCodigo();
        franja.setEstado(estado);
        return franja;
    }

    private void existe(Franja franja) {
        when(franjaRepository.findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(franja.getCodigoFranja(),
            instructor)).thenReturn(Optional.of(franja));
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---------------------------------------------------------------- bloquear

    @Test
    void bloquear_franjaLibreFutura_laCambiaABloqueadaYLaDevuelve() {
        Franja libre = franja(HOY, "10:00", EstadoFranja.LIBRE);
        Franja bloqueada = franja(HOY, "10:00", EstadoFranja.BLOQUEADA);
        existe(libre);
        when(franjaRepository.cambiarEstado(libre.getCodigoFranja(), instructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA)).thenReturn(Optional.of(bloqueada));

        FranjaResponse respuesta = service.bloquear(instructor, libre.getCodigoFranja());

        assertThat(respuesta.estado()).isEqualTo(EstadoFranja.BLOQUEADA);
        assertThat(respuesta.horaInicio()).isEqualTo("10:00");
    }

    @Test
    void bloquear_franjaReservada_lanzaErrAca08SinCambios() {
        Franja reservada = franja(HOY.plusDays(1), "10:00", EstadoFranja.RESERVADA);
        existe(reservada);
        when(franjaRepository.cambiarEstado(any(), any(), eq(EstadoFranja.LIBRE), eq(EstadoFranja.BLOQUEADA)))
            .thenReturn(Optional.empty());

        assertError(() -> service.bloquear(instructor, reservada.getCodigoFranja()), "ERR_ACA_08");
    }

    @Test
    void bloquear_siCambioDeEstadoEntreLecturaYEscritura_lanzaErrAca08() {
        Franja libre = franja(HOY.plusDays(1), "10:00", EstadoFranja.LIBRE);
        existe(libre);
        // * Otra petición la reservó justo antes: el cambio condicional no encuentra nada
        when(franjaRepository.cambiarEstado(any(), any(), eq(EstadoFranja.LIBRE), eq(EstadoFranja.BLOQUEADA)))
            .thenReturn(Optional.empty());

        assertError(() -> service.bloquear(instructor, libre.getCodigoFranja()), "ERR_ACA_08");
    }

    @Test
    void bloquear_inexistenteODeOtroInstructor_lanzaErrAca07() {
        UUID codigo = UUID.randomUUID();
        when(franjaRepository.findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(codigo, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.bloquear(instructor, codigo), "ERR_ACA_07");
        verify(franjaRepository, never()).cambiarEstado(any(), any(), any(), any());
    }

    @Test
    void bloquear_franjaQueYaEmpezo_lanzaErrAca10() {
        Franja pasada = franja(HOY, "09:00", EstadoFranja.LIBRE);
        existe(pasada);

        assertError(() -> service.bloquear(instructor, pasada.getCodigoFranja()), "ERR_ACA_10");
        verify(franjaRepository, never()).cambiarEstado(any(), any(), any(), any());
    }

    @Test
    void bloquear_franjaQueEmpiezaExactamenteAhora_lanzaErrAca10() {
        Franja ahora = franja(HOY, "09:30", EstadoFranja.LIBRE);
        existe(ahora);

        assertError(() -> service.bloquear(instructor, ahora.getCodigoFranja()), "ERR_ACA_10");
    }

    @Test
    void bloquear_franjaDeUnDiaPasado_lanzaErrAca10() {
        Franja ayer = franja(HOY.minusDays(1), "23:00", EstadoFranja.LIBRE);
        existe(ayer);

        assertError(() -> service.bloquear(instructor, ayer.getCodigoFranja()), "ERR_ACA_10");
    }

    // ---------------------------------------------------------------- desbloquear

    @Test
    void desbloquear_franjaBloqueadaFutura_laCambiaALibre() {
        Franja bloqueada = franja(HOY.plusDays(1), "10:00", EstadoFranja.BLOQUEADA);
        Franja libre = franja(HOY.plusDays(1), "10:00", EstadoFranja.LIBRE);
        existe(bloqueada);
        when(franjaRepository.cambiarEstado(bloqueada.getCodigoFranja(), instructor, EstadoFranja.BLOQUEADA,
            EstadoFranja.LIBRE)).thenReturn(Optional.of(libre));

        FranjaResponse respuesta = service.desbloquear(instructor, bloqueada.getCodigoFranja());

        assertThat(respuesta.estado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void desbloquear_franjaQueNoEstaBloqueada_lanzaErrAca09() {
        Franja libre = franja(HOY.plusDays(1), "10:00", EstadoFranja.LIBRE);
        existe(libre);
        when(franjaRepository.cambiarEstado(any(), any(), eq(EstadoFranja.BLOQUEADA), eq(EstadoFranja.LIBRE)))
            .thenReturn(Optional.empty());

        assertError(() -> service.desbloquear(instructor, libre.getCodigoFranja()), "ERR_ACA_09");
    }

    @Test
    void desbloquear_inexistente_lanzaErrAca07() {
        UUID codigo = UUID.randomUUID();
        when(franjaRepository.findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(codigo, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.desbloquear(instructor, codigo), "ERR_ACA_07");
    }

    @Test
    void desbloquear_franjaQueYaEmpezo_lanzaErrAca10() {
        Franja pasada = franja(HOY, "08:00", EstadoFranja.BLOQUEADA);
        existe(pasada);

        assertError(() -> service.desbloquear(instructor, pasada.getCodigoFranja()), "ERR_ACA_10");
        verify(franjaRepository, never()).cambiarEstado(any(), any(), any(), any());
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listar_conUnRangoValido_lasPasaTalCual() {
        LocalDate desde = HOY.plusDays(7);
        LocalDate hasta = desde.plusDays(6);
        when(franjaRepository.listar(eq(instructor), eq(desde), eq(hasta), eq(null), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(franja(desde, "10:00", EstadoFranja.LIBRE))));

        var pagina = service.listar(instructor, desde, hasta, null, 1, 20);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).fecha()).isEqualTo(desde);
    }

    @Test
    void listar_unSoloDia_esUnRangoDeUnDia() {
        when(franjaRepository.listar(eq(instructor), eq(HOY), eq(HOY), eq(null), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, HOY, HOY, null, 1, 20);

        verify(franjaRepository).listar(eq(instructor), eq(HOY), eq(HOY), eq(null), any(Pageable.class));
    }

    @Test
    void listar_rangoDeSieteDias_esValidoYDeOchoNo() {
        when(franjaRepository.listar(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, HOY, HOY.plusDays(6), null, 1, 20);

        assertError(() -> service.listar(instructor, HOY, HOY.plusDays(7), null, 1, 20), "ERR_ACA_13");
    }

    @Test
    void listar_fechaInicialPosteriorALaFinal_lanzaErrAca13() {
        assertError(() -> service.listar(instructor, HOY.plusDays(2), HOY, null, 1, 20), "ERR_ACA_13");
        verify(franjaRepository, never()).listar(any(), any(), any(), any(), any());
    }

    @Test
    void listar_conUnaSolaFecha_lanzaErrAca13() {
        assertError(() -> service.listar(instructor, HOY, null, null, 1, 20), "ERR_ACA_13");
        assertError(() -> service.listar(instructor, null, HOY, null, 1, 20), "ERR_ACA_13");
        verify(franjaRepository, never()).listar(any(), any(), any(), any(), any());
    }

    @Test
    void listar_sinFechas_pideDesdeHoySinTope() {
        when(franjaRepository.listar(eq(instructor), eq(HOY), eq(null), eq(EstadoFranja.LIBRE),
            any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, null, null, EstadoFranja.LIBRE, 1, 20);

        verify(franjaRepository).listar(eq(instructor), eq(HOY), eq(null), eq(EstadoFranja.LIBRE),
            any(Pageable.class));
    }

    @Test
    void listar_ordenaPorFechaYHoraYNormalizaLaPaginacion() {
        when(franjaRepository.listar(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, null, null, null, 0, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(franjaRepository).listar(any(), any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().stream().map(o -> o.getProperty()).toList())
            .containsExactly("fecha", "horaInicio");
    }

    // ---------------------------------------------------------------- bloquear y desbloquear un dia

    @Test
    void bloquearDia_unDiaFuturo_cambiaLasLibresSinHoraMinima() {
        LocalDate manana = HOY.plusDays(1);
        when(franjaRepository.cambiarEstadoDelDia(instructor, manana, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA,
            null)).thenReturn(4L);

        DiaBloqueadoResponse respuesta = service.bloquearDia(instructor, manana);

        assertThat(respuesta.fecha()).isEqualTo(manana);
        assertThat(respuesta.franjasAfectadas()).isEqualTo(4);
    }

    @Test
    void bloquearDia_hoy_soloTocaLasQueEmpiezanDespuesDeLaHoraActual() {
        when(franjaRepository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA,
            "09:30")).thenReturn(2L);

        assertThat(service.bloquearDia(instructor, HOY).franjasAfectadas()).isEqualTo(2);
    }

    @Test
    void bloquearDia_sinFranjasLibres_informaCeroSinError() {
        when(franjaRepository.cambiarEstadoDelDia(any(), any(), any(), any(), any())).thenReturn(0L);

        assertThat(service.bloquearDia(instructor, HOY.plusDays(3)).franjasAfectadas()).isZero();
    }

    @Test
    void bloquearDia_repetirlo_noCambiaNadaMas() {
        LocalDate manana = HOY.plusDays(1);
        when(franjaRepository.cambiarEstadoDelDia(instructor, manana, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA,
            null)).thenReturn(3L).thenReturn(0L);

        assertThat(service.bloquearDia(instructor, manana).franjasAfectadas()).isEqualTo(3);
        assertThat(service.bloquearDia(instructor, manana).franjasAfectadas()).isZero();
    }

    @Test
    void bloquearDia_fechaPasada_lanzaErrAca10SinTocarNada() {
        assertError(() -> service.bloquearDia(instructor, HOY.minusDays(1)), "ERR_ACA_10");
        verify(franjaRepository, never()).cambiarEstadoDelDia(any(), any(), any(), any(), any());
    }

    @Test
    void desbloquearDia_cambiaLasBloqueadasALibres() {
        LocalDate manana = HOY.plusDays(1);
        when(franjaRepository.cambiarEstadoDelDia(instructor, manana, EstadoFranja.BLOQUEADA, EstadoFranja.LIBRE,
            null)).thenReturn(4L);

        assertThat(service.desbloquearDia(instructor, manana).franjasAfectadas()).isEqualTo(4);
    }

    @Test
    void desbloquearDia_hoyYFechaPasada() {
        when(franjaRepository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.BLOQUEADA, EstadoFranja.LIBRE,
            "09:30")).thenReturn(1L);

        assertThat(service.desbloquearDia(instructor, HOY).franjasAfectadas()).isEqualTo(1);
        assertError(() -> service.desbloquearDia(instructor, HOY.minusDays(1)), "ERR_ACA_10");
    }
}
