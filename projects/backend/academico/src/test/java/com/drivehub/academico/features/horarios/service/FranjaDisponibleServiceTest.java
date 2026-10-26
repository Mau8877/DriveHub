package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.UUID;

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
import com.drivehub.academico.features.horarios.dto.FranjaDisponibleResponse;
import com.drivehub.academico.features.horarios.dto.InstructorDisponibleResponse;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize).
 */
@ExtendWith(MockitoExtension.class)
class FranjaDisponibleServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final String BEARER = "Bearer token";

    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private IdentidadClient identidadClient;

    private final UUID escuela = UUID.randomUUID();
    private final UUID instructorA = UUID.randomUUID();
    private final UUID instructorB = UUID.randomUUID();
    private FranjaDisponibleService service;

    @BeforeEach
    void setUp() {
        service = new FranjaDisponibleService(franjaRepository, identidadClient,
            Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
    }

    private Franja franja(UUID instructor, LocalDate fecha, String inicio, String fin) {
        Franja franja = new Franja(escuela, 1L, instructor, fecha, inicio, fin);
        franja.inicializarCodigo();
        return franja;
    }

    private Page<FranjaDisponibleResponse> listar(LocalDate desde, LocalDate hasta, UUID instructor, String horaDesde,
            String horaHasta) {
        return service.listar(escuela, BEARER, desde, hasta, instructor, horaDesde, horaHasta, 1, 20);
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listar_pideALaBaseConLaEscuelaYElAhoraDelReloj_yUneLosNombres() {
        Franja a = franja(instructorA, HOY, "10:00", "11:00");
        Franja b = franja(instructorB, HOY.plusDays(1), "08:00", "09:00");
        when(franjaRepository.disponibles(eq(escuela), eq(HOY), eq("09:30"), any(), any(), any(), any(), any(),
            any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a, b), PageRequest.of(0, 20), 2));
        when(identidadClient.nombresDeInstructores(any(), eq(BEARER)))
            .thenReturn(Map.of(instructorA, "Ana Alfa", instructorB, "Beto Beta"));

        Page<FranjaDisponibleResponse> resultado = listar(null, null, null, null, null);

        assertThat(resultado.getContent()).extracting(FranjaDisponibleResponse::instructorNombre,
            FranjaDisponibleResponse::horaInicio).containsExactly(
            org.assertj.core.groups.Tuple.tuple("Ana Alfa", "10:00"),
            org.assertj.core.groups.Tuple.tuple("Beto Beta", "08:00"));
        assertThat(resultado.getContent().get(0).codigo()).isEqualTo(a.getCodigoFranja());
        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listar_pasaLosFiltrosYUnaPaginaNormalizada() {
        when(franjaRepository.disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listar(escuela, BEARER, HOY.plusDays(1), HOY.plusDays(3), instructorA, "12:00", "18:00", 0, 500);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(franjaRepository).disponibles(eq(escuela), eq(HOY), eq("09:30"), eq(HOY.plusDays(1)),
            eq(HOY.plusDays(3)), eq(instructorA), eq("12:00"), eq("18:00"), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isZero();
        assertThat(pagina.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listar_sinFranjas_devuelveVacioYNoLlamaAIdentidad() {
        when(franjaRepository.disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(listar(null, null, null, null, null).getContent()).isEmpty();
        verifyNoInteractions(identidadClient);
    }

    @Test
    void listar_unInstructorSinNombre_semuestraComoInstructor() {
        when(franjaRepository.disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(franja(instructorA, HOY, "10:00", "11:00")),
                PageRequest.of(0, 20), 1));
        when(identidadClient.nombresDeInstructores(any(), any())).thenReturn(Map.of());

        assertThat(listar(null, null, null, null, null).getContent().get(0).instructorNombre())
            .isEqualTo("Instructor");
    }

    @Test
    void listar_siIdentidadNoResponde_fallaConErrAca17() {
        when(franjaRepository.disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(franja(instructorA, HOY, "10:00", "11:00")),
                PageRequest.of(0, 20), 1));
        when(identidadClient.nombresDeInstructores(any(), any()))
            .thenThrow(new ServicioNoDisponibleException("ERR_ACA_17", "No"));

        assertError(() -> listar(null, null, null, null, null), "ERR_ACA_17");
    }

    @Test
    void listar_sinEscuelaEnElToken_lanzaErrAca16() {
        assertError(() -> service.listar(null, BEARER, null, null, null, null, null, 1, 20), "ERR_ACA_16");
        verifyNoInteractions(franjaRepository);
    }

    @Test
    void listar_conUnRangoInvalido_lanzaErrAca13() {
        // * solo una fecha, al revés, y de 29 días
        assertError(() -> listar(HOY, null, null, null, null), "ERR_ACA_13");
        assertError(() -> listar(null, HOY, null, null, null), "ERR_ACA_13");
        assertError(() -> listar(HOY.plusDays(2), HOY, null, null, null), "ERR_ACA_13");
        assertError(() -> listar(HOY, HOY.plusDays(28), null, null, null), "ERR_ACA_13");
        verify(franjaRepository, never()).disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listar_unRangoDeExactamente28Dias_seAcepta() {
        when(franjaRepository.disponibles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(listar(HOY, HOY.plusDays(27), null, null, null).getContent()).isEmpty();
    }

    @Test
    void listar_conUnaHoraMalFormada_lanzaErrSys01() {
        assertError(() -> listar(null, null, null, "8:00", null), "ERR_SYS_01");
        assertError(() -> listar(null, null, null, null, "25:00"), "ERR_SYS_01");
        assertError(() -> listar(null, null, null, "mañana", null), "ERR_SYS_01");
        verifyNoInteractions(franjaRepository);
    }

    // ---------------------------------------------------------------- instructores

    @Test
    void instructores_losOrdenaPorNombre_yUsaElNombreDeRespaldo() {
        when(franjaRepository.instructoresConDisponibles(escuela, HOY, "09:30", null, null))
            .thenReturn(List.of(instructorA, instructorB));
        when(identidadClient.nombresDeInstructores(any(), eq(BEARER))).thenReturn(Map.of(instructorB, "Ana Alfa"));

        List<InstructorDisponibleResponse> resultado = service.instructores(escuela, BEARER, null, null);

        assertThat(resultado).extracting(InstructorDisponibleResponse::nombre).containsExactly("Ana Alfa", "Instructor");
        assertThat(resultado.get(0).codigo()).isEqualTo(instructorB);
    }

    @Test
    void instructores_sinNinguno_devuelveVacioYNoLlamaAIdentidad() {
        when(franjaRepository.instructoresConDisponibles(any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThat(service.instructores(escuela, BEARER, null, null)).isEmpty();
        verifyNoInteractions(identidadClient);
    }

    @Test
    void instructores_sinEscuelaOConUnRangoInvalido_falla() {
        assertError(() -> service.instructores(null, BEARER, null, null), "ERR_ACA_16");
        assertError(() -> service.instructores(escuela, BEARER, HOY, null), "ERR_ACA_13");
    }
}
