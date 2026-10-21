package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.horarios.dto.CrearPlantillaHorarioRequest;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioCreadaResponse;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioResponse;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;

@ExtendWith(MockitoExtension.class)
class PlantillaHorarioServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 09:30
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final LocalDate HOY = AHORA.toLocalDate();

    @Mock
    private PlantillaHorarioRepository plantillaRepository;
    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private FranjaGeneracionService generacion;

    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private PlantillaHorarioService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new PlantillaHorarioService(plantillaRepository, franjaRepository, generacion, reloj);
    }

    private static CrearPlantillaHorarioRequest request(int dia, String inicio, String fin, int duracion,
            LocalDate desde, LocalDate hasta) {
        return new CrearPlantillaHorarioRequest(dia, inicio, fin, duracion, desde, hasta);
    }

    private PlantillaHorario existente(int dia, String inicio, String fin, LocalDate desde, LocalDate hasta) {
        PlantillaHorario horario = new PlantillaHorario(escuela, instructor, dia, inicio, fin, 60, desde, hasta);
        horario.inicializarCodigo();
        horario.setId(99L);
        return horario;
    }

    private void guardarDevuelveElMismoConId() {
        when(plantillaRepository.save(any(PlantillaHorario.class))).thenAnswer(inv -> {
            PlantillaHorario horario = inv.getArgument(0);
            horario.inicializarCodigo();
            horario.setId(1L);
            return horario;
        });
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
        verify(plantillaRepository, never()).save(any());
        verify(generacion, never()).generar(any());
    }

    // ---------------------------------------------------------------- crear

    @Test
    void crear_horarioValido_guardaActivoConEscuelaYGeneraLasFranjas() {
        guardarDevuelveElMismoConId();
        when(generacion.generar(any())).thenReturn(14);

        PlantillaHorarioCreadaResponse respuesta = service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, null, null));

        ArgumentCaptor<PlantillaHorario> guardado = ArgumentCaptor.forClass(PlantillaHorario.class);
        verify(plantillaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(guardado.getValue().getInstructorCodigo()).isEqualTo(instructor);
        assertThat(guardado.getValue().isActiva()).isTrue();
        assertThat(guardado.getValue().getVigenteDesde()).isEqualTo(HOY);
        assertThat(respuesta.franjasGeneradas()).isEqualTo(14);
        assertThat(respuesta.codigo()).isNotNull();
        assertThat(respuesta.horaInicio()).isEqualTo("08:00");
        assertThat(respuesta.activa()).isTrue();
    }

    @Test
    void crear_franjasGeneradasEsLoQueInformaElGenerador() {
        guardarDevuelveElMismoConId();
        when(generacion.generar(any())).thenReturn(3);

        assertThat(service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, null, null))
            .franjasGeneradas()).isEqualTo(3);
    }

    @Test
    void crear_vigenteDesdeNulo_tomaHoy() {
        guardarDevuelveElMismoConId();

        service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, null, null));

        ArgumentCaptor<PlantillaHorario> guardado = ArgumentCaptor.forClass(PlantillaHorario.class);
        verify(plantillaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getVigenteDesde()).isEqualTo(HOY);
    }

    @Test
    void crear_vigenteDesdeFuturo_loRespeta() {
        guardarDevuelveElMismoConId();
        LocalDate futuro = HOY.plusDays(10);

        service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, futuro, null));

        ArgumentCaptor<PlantillaHorario> guardado = ArgumentCaptor.forClass(PlantillaHorario.class);
        verify(plantillaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getVigenteDesde()).isEqualTo(futuro);
    }

    @Test
    void crear_horaFinNoPosterior_lanzaErrAca01() {
        assertError(() -> service.crear(instructor, escuela, request(1, "12:00", "12:00", 60, null, null)),
            "ERR_ACA_01");
        assertError(() -> service.crear(instructor, escuela, request(1, "12:00", "08:00", 60, null, null)),
            "ERR_ACA_01");
    }

    @Test
    void crear_duracionFueraDeRango_lanzaErrAca02() {
        assertError(() -> service.crear(instructor, escuela, request(1, "08:00", "12:00", 14, null, null)),
            "ERR_ACA_02");
        assertError(() -> service.crear(instructor, escuela, request(1, "08:00", "12:00", 241, null, null)),
            "ERR_ACA_02");
    }

    @Test
    void crear_duracionEnLosLimites_esValida() {
        guardarDevuelveElMismoConId();

        service.crear(instructor, escuela, request(1, "08:00", "08:15", 15, null, null));
        service.crear(instructor, escuela, request(1, "08:00", "12:00", 240, null, null));

        verify(plantillaRepository, times(2)).save(any());
    }

    @Test
    void crear_tramoQueNoEsMultiploDeLaDuracion_lanzaErrAca02() {
        assertError(() -> service.crear(instructor, escuela, request(1, "08:00", "12:00", 90, null, null)),
            "ERR_ACA_02");
    }

    @Test
    void crear_vigenteDesdeAnteriorAHoy_lanzaErrAca03() {
        assertError(() -> service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, HOY.minusDays(1), null)), "ERR_ACA_03");
    }

    @Test
    void crear_vigenteHastaAnteriorAVigenteDesde_lanzaErrAca03() {
        assertError(() -> service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, HOY.plusDays(5), HOY.plusDays(4))), "ERR_ACA_03");
        assertError(() -> service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, null, HOY.minusDays(1))), "ERR_ACA_03");
    }

    @Test
    void crear_vigenteHastaIgualAVigenteDesde_esValida() {
        guardarDevuelveElMismoConId();

        service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, HOY, HOY));

        verify(plantillaRepository).save(any());
    }

    @Test
    void crear_seCruzaConOtroHorarioActivo_lanzaErrAca04() {
        when(plantillaRepository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .thenReturn(List.of(existente(1, "11:00", "13:00", HOY, null)));

        assertError(() -> service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, null, null)),
            "ERR_ACA_04");
    }

    @Test
    void crear_unHorarioContenidoEnOtro_seCruza() {
        when(plantillaRepository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .thenReturn(List.of(existente(1, "08:00", "18:00", HOY, null)));

        assertError(() -> service.crear(instructor, escuela, request(1, "10:00", "11:00", 60, null, null)),
            "ERR_ACA_04");
    }

    @Test
    void crear_ventanaQueSoloComparteElBorde_noSeCruza() {
        guardarDevuelveElMismoConId();
        when(plantillaRepository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .thenReturn(List.of(existente(1, "08:00", "12:00", HOY, null)));

        service.crear(instructor, escuela, request(1, "12:00", "14:00", 60, null, null));
        service.crear(instructor, escuela, request(1, "06:00", "08:00", 60, null, null));

        verify(plantillaRepository, times(2)).save(any());
    }

    @Test
    void crear_mismasHorasPeroVigenciasDisjuntas_noSeCruza() {
        guardarDevuelveElMismoConId();
        when(plantillaRepository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .thenReturn(List.of(existente(1, "08:00", "12:00", HOY, HOY.plusDays(6))));

        service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, HOY.plusDays(7), null));

        verify(plantillaRepository).save(any());
    }

    @Test
    void crear_vigenciaAbiertaDelOtroSeSolapaConLaNueva_seCruza() {
        // * El otro no tiene fin: cubre cualquier fecha futura
        when(plantillaRepository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .thenReturn(List.of(existente(1, "08:00", "12:00", HOY, null)));

        assertError(() -> service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, HOY.plusDays(100), HOY.plusDays(110))), "ERR_ACA_04");
    }

    @Test
    void crear_consultaElCruceSoloDelMismoInstructorYDia() {
        guardarDevuelveElMismoConId();

        service.crear(instructor, escuela, request(3, "08:00", "12:00", 60, null, null));

        verify(plantillaRepository)
            .findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 3);
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listar_sinFiltroDeActiva_pideTodosOrdenadosPorDiaYHora() {
        when(plantillaRepository.findByInstructorCodigoAndDeletedAtIsNull(eq(instructor), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(existente(1, "08:00", "12:00", HOY, null))));

        Page<PlantillaHorarioResponse> pagina = service.listar(instructor, null, 1, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(plantillaRepository).findByInstructorCodigoAndDeletedAtIsNull(eq(instructor), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().stream().map(o -> o.getProperty()).toList())
            .containsExactly("diaSemana", "horaInicio");
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).horaFin()).isEqualTo("12:00");
    }

    @Test
    void listar_conFiltroDeActiva_loPasaAlRepositorio() {
        when(plantillaRepository.findByInstructorCodigoAndActivaAndDeletedAtIsNull(eq(instructor), eq(true),
            any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, true, 1, 20);

        verify(plantillaRepository).findByInstructorCodigoAndActivaAndDeletedAtIsNull(eq(instructor), eq(true),
            any(Pageable.class));
        verify(plantillaRepository, never()).findByInstructorCodigoAndDeletedAtIsNull(any(), any());
    }

    @Test
    void listar_normalizaLaPaginacion() {
        when(plantillaRepository.findByInstructorCodigoAndDeletedAtIsNull(eq(instructor), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.listar(instructor, null, 0, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(plantillaRepository).findByInstructorCodigoAndDeletedAtIsNull(eq(instructor), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------- desactivar

    @Test
    void desactivar_eliminaLasFranjasFuturasYAlFinalMarcaInactivo() {
        PlantillaHorario horario = existente(1, "08:00", "12:00", HOY, null);
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).thenReturn(Optional.of(horario));
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString())).thenReturn(9L);

        service.desactivar(instructor, horario.getCodigoPlantillaHorario());

        InOrder orden = inOrder(franjaRepository, plantillaRepository);
        orden.verify(franjaRepository).eliminarFuturasNoReservadas(99L, HOY, "09:30");
        orden.verify(plantillaRepository).save(horario);
        assertThat(horario.isActiva()).isFalse();
    }

    @Test
    void desactivar_siElBorradoFalla_noMarcaInactivo() {
        PlantillaHorario horario = existente(1, "08:00", "12:00", HOY, null);
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).thenReturn(Optional.of(horario));
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString()))
            .thenThrow(new IllegalStateException("la base no responde"));

        assertThatThrownBy(() -> service.desactivar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(horario.isActiva()).isTrue();
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void desactivar_horarioInexistenteODeOtroInstructor_lanzaErrAca05() {
        UUID codigo = UUID.randomUUID();
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(codigo,
            instructor)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(instructor, codigo)).isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_05"));
        verify(franjaRepository, never()).eliminarFuturasNoReservadas(anyLong(), any(), anyString());
    }

    @Test
    void desactivar_horarioYaInactivo_lanzaErrAca06SinTocarFranjas() {
        PlantillaHorario horario = existente(1, "08:00", "12:00", HOY, null);
        horario.setActiva(false);
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).thenReturn(Optional.of(horario));

        assertThatThrownBy(() -> service.desactivar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_06"));
        verify(franjaRepository, never()).eliminarFuturasNoReservadas(anyLong(), any(), anyString());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void desactivar_noGeneraFranjas() {
        PlantillaHorario horario = existente(1, "08:00", "12:00", HOY, null);
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).thenReturn(Optional.of(horario));

        service.desactivar(instructor, horario.getCodigoPlantillaHorario());

        verify(generacion, never()).generar(any());
    }
}
