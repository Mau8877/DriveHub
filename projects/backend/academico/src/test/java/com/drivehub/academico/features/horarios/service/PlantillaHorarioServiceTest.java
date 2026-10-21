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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.horarios.dto.CrearPlantillaHorarioRequest;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioCreadaResponse;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioResponse;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
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

    private static CrearPlantillaHorarioRequest request(List<Integer> dias, String inicio, String fin, int duracion,
            LocalDate desde, LocalDate hasta) {
        return new CrearPlantillaHorarioRequest("Mañanas", dias, inicio, fin, duracion, desde, hasta);
    }

    private static CrearPlantillaHorarioRequest request(int dia, String inicio, String fin, int duracion,
            LocalDate desde, LocalDate hasta) {
        return request(List.of(dia), inicio, fin, duracion, desde, hasta);
    }

    private PlantillaHorario existente(List<Integer> dias, String inicio, String fin, LocalDate desde,
            LocalDate hasta) {
        PlantillaHorario horario = new PlantillaHorario(escuela, instructor, "Existente", dias, inicio, fin, 60,
            desde, hasta);
        horario.inicializarCodigo();
        horario.setId(99L);
        return horario;
    }

    private PlantillaHorario existente(int dia, String inicio, String fin, LocalDate desde, LocalDate hasta) {
        return existente(List.of(dia), inicio, fin, desde, hasta);
    }

    private PlantillaHorario conEstado(EstadoPlantilla estado) {
        PlantillaHorario horario = existente(1, "08:00", "12:00", HOY, null);
        horario.setEstado(estado);
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).thenReturn(Optional.of(horario));
        return horario;
    }

    private void activos(PlantillaHorario... horarios) {
        when(plantillaRepository.findByInstructorCodigoAndEstadoAndDeletedAtIsNull(instructor,
            EstadoPlantilla.ACTIVA)).thenReturn(List.of(horarios));
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
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
        assertThat(guardado.getValue().getVigenteDesde()).isEqualTo(HOY);
        assertThat(respuesta.franjasGeneradas()).isEqualTo(14);
        assertThat(respuesta.codigo()).isNotNull();
        assertThat(respuesta.horaInicio()).isEqualTo("08:00");
        assertThat(respuesta.estado()).isEqualTo(EstadoPlantilla.ACTIVA);
    }

    @Test
    void crear_variosDias_losGuardaSinRepetirOrdenadosYConElNombreRecortado() {
        guardarDevuelveElMismoConId();
        var pedido = new CrearPlantillaHorarioRequest("  Entre semana  ", List.of(5, 1, 3, 1), "08:00", "12:00", 60,
            null, null);

        PlantillaHorarioCreadaResponse respuesta = service.crear(instructor, escuela, pedido);

        ArgumentCaptor<PlantillaHorario> guardado = ArgumentCaptor.forClass(PlantillaHorario.class);
        verify(plantillaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getDiasSemana()).containsExactly(1, 3, 5);
        assertThat(guardado.getValue().getNombre()).isEqualTo("Entre semana");
        assertThat(respuesta.diasSemana()).containsExactly(1, 3, 5);
        assertThat(respuesta.nombre()).isEqualTo("Entre semana");
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
        activos(existente(1, "11:00", "13:00", HOY, null));

        assertError(() -> service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, null, null)),
            "ERR_ACA_04");
    }

    @Test
    void crear_unHorarioContenidoEnOtro_seCruza() {
        activos(existente(1, "08:00", "18:00", HOY, null));

        assertError(() -> service.crear(instructor, escuela, request(1, "10:00", "11:00", 60, null, null)),
            "ERR_ACA_04");
    }

    @Test
    void crear_ventanaQueSoloComparteElBorde_noSeCruza() {
        guardarDevuelveElMismoConId();
        activos(existente(1, "08:00", "12:00", HOY, null));

        service.crear(instructor, escuela, request(1, "12:00", "14:00", 60, null, null));
        service.crear(instructor, escuela, request(1, "06:00", "08:00", 60, null, null));

        verify(plantillaRepository, times(2)).save(any());
    }

    @Test
    void crear_mismasHorasPeroVigenciasDisjuntas_noSeCruza() {
        guardarDevuelveElMismoConId();
        activos(existente(1, "08:00", "12:00", HOY, HOY.plusDays(6)));

        service.crear(instructor, escuela, request(1, "08:00", "12:00", 60, HOY.plusDays(7), null));

        verify(plantillaRepository).save(any());
    }

    @Test
    void crear_vigenciaAbiertaDelOtroSeSolapaConLaNueva_seCruza() {
        // * El otro no tiene fin: cubre cualquier fecha futura
        activos(existente(1, "08:00", "12:00", HOY, null));

        assertError(() -> service.crear(instructor, escuela,
            request(1, "08:00", "12:00", 60, HOY.plusDays(100), HOY.plusDays(110))), "ERR_ACA_04");
    }

    @Test
    void crear_mismasHorasPeroSinNingunDiaEnComun_noSeCruza() {
        guardarDevuelveElMismoConId();
        activos(existente(List.of(1, 2, 3), "08:00", "12:00", HOY, null));

        service.crear(instructor, escuela, request(List.of(4, 5, 6), "08:00", "12:00", 60, null, null));

        verify(plantillaRepository).save(any());
    }

    @Test
    void crear_compartiendoSoloUnDiaDeVarios_seCruza() {
        activos(existente(List.of(1, 2, 3), "08:00", "12:00", HOY, null));

        assertError(() -> service.crear(instructor, escuela,
            request(List.of(3, 4, 5), "10:00", "12:00", 60, null, null)), "ERR_ACA_04");
    }

    @Test
    void crear_consultaElCruceSoloContraLosActivosDelInstructor() {
        guardarDevuelveElMismoConId();

        service.crear(instructor, escuela, request(3, "08:00", "12:00", 60, null, null));

        verify(plantillaRepository).findByInstructorCodigoAndEstadoAndDeletedAtIsNull(instructor,
            EstadoPlantilla.ACTIVA);
    }

    // ---------------------------------------------------------------- listar

    private PlantillaHorario conEstadoYDia(EstadoPlantilla estado, int dia, String inicio) {
        PlantillaHorario horario = existente(dia, inicio, "12:00", HOY, null);
        horario.setEstado(estado);
        return horario;
    }

    @Test
    void listar_sinEstados_pideActivosYPausados() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of());

        service.listar(instructor, Set.of(), 1, 20);

        verify(plantillaRepository).findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructor,
            EnumSet.of(EstadoPlantilla.ACTIVA, EstadoPlantilla.PAUSADA));
    }

    @Test
    void listar_conEstados_lospasaAlRepositorio() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of());

        service.listar(instructor, EnumSet.of(EstadoPlantilla.INACTIVA), 1, 20);

        verify(plantillaRepository).findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructor,
            EnumSet.of(EstadoPlantilla.INACTIVA));
    }

    @Test
    void listar_ordenaPorEstadoLuegoPorPrimerDiaYHora() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of(
                conEstadoYDia(EstadoPlantilla.INACTIVA, 1, "08:00"),
                conEstadoYDia(EstadoPlantilla.PAUSADA, 2, "08:00"),
                conEstadoYDia(EstadoPlantilla.ACTIVA, 3, "08:00"),
                conEstadoYDia(EstadoPlantilla.ACTIVA, 1, "14:00"),
                conEstadoYDia(EstadoPlantilla.ACTIVA, 1, "08:00")));

        Page<PlantillaHorarioResponse> pagina = service.listar(instructor, EnumSet.allOf(EstadoPlantilla.class),
            1, 20);

        assertThat(pagina.getContent()).extracting(PlantillaHorarioResponse::estado, r -> r.diasSemana().get(0),
            PlantillaHorarioResponse::horaInicio).containsExactly(
                org.assertj.core.api.Assertions.tuple(EstadoPlantilla.ACTIVA, 1, "08:00"),
                org.assertj.core.api.Assertions.tuple(EstadoPlantilla.ACTIVA, 1, "14:00"),
                org.assertj.core.api.Assertions.tuple(EstadoPlantilla.ACTIVA, 3, "08:00"),
                org.assertj.core.api.Assertions.tuple(EstadoPlantilla.PAUSADA, 2, "08:00"),
                org.assertj.core.api.Assertions.tuple(EstadoPlantilla.INACTIVA, 1, "08:00"));
    }

    @Test
    void listar_paginaEnMemoriaYInformaElTotal() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of(
                conEstadoYDia(EstadoPlantilla.ACTIVA, 1, "08:00"),
                conEstadoYDia(EstadoPlantilla.ACTIVA, 2, "08:00"),
                conEstadoYDia(EstadoPlantilla.ACTIVA, 3, "08:00")));

        Page<PlantillaHorarioResponse> segunda = service.listar(instructor, Set.of(), 2, 2);

        assertThat(segunda.getTotalElements()).isEqualTo(3);
        assertThat(segunda.getTotalPages()).isEqualTo(2);
        assertThat(segunda.getNumber()).isEqualTo(1);
        assertThat(segunda.getContent()).hasSize(1);
    }

    @Test
    void listar_paginaFueraDeRango_devuelveVacioSinFallar() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of(conEstadoYDia(EstadoPlantilla.ACTIVA, 1, "08:00")));

        Page<PlantillaHorarioResponse> pagina = service.listar(instructor, Set.of(), 9, 20);

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listar_normalizaLaPaginacion() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(eq(instructor), any()))
            .thenReturn(List.of());

        Page<PlantillaHorarioResponse> pagina = service.listar(instructor, Set.of(), 0, 5000);

        assertThat(pagina.getNumber()).isZero();
        assertThat(pagina.getSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------- pausar

    @Test
    void pausar_eliminaLasFranjasFuturasYAlFinalMarcaPausada() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.ACTIVA);
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString())).thenReturn(9L);

        service.pausar(instructor, horario.getCodigoPlantillaHorario());

        InOrder orden = inOrder(franjaRepository, plantillaRepository);
        orden.verify(franjaRepository).eliminarFuturasNoReservadas(99L, HOY, "09:30");
        orden.verify(plantillaRepository).save(horario);
        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.PAUSADA);
    }

    @Test
    void pausar_siElBorradoFalla_sigueActivo() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.ACTIVA);
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString()))
            .thenThrow(new IllegalStateException("la base no responde"));

        assertThatThrownBy(() -> service.pausar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void pausar_horarioPausadoOInactivo_lanzaErrAca11SinTocarFranjas() {
        for (EstadoPlantilla estado : List.of(EstadoPlantilla.PAUSADA, EstadoPlantilla.INACTIVA)) {
            PlantillaHorario horario = conEstado(estado);

            assertThatThrownBy(() -> service.pausar(instructor, horario.getCodigoPlantillaHorario()))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_11"));
        }
        verify(franjaRepository, never()).eliminarFuturasNoReservadas(anyLong(), any(), anyString());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void pausar_horarioInexistenteODeOtroInstructor_lanzaErrAca05() {
        UUID codigo = UUID.randomUUID();
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(codigo,
            instructor)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pausar(instructor, codigo)).isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_05"));
    }

    // ---------------------------------------------------------------- reanudar

    @Test
    void reanudar_horarioPausado_quedaActivoYGeneraLasFranjas() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.PAUSADA);
        activos();
        when(generacion.generar(horario)).thenReturn(16);

        service.reanudar(instructor, horario.getCodigoPlantillaHorario());

        InOrder orden = inOrder(plantillaRepository, generacion);
        orden.verify(plantillaRepository).save(horario);
        orden.verify(generacion).generar(horario);
        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
    }

    @Test
    void reanudar_horarioActivoOInactivo_lanzaErrAca12() {
        for (EstadoPlantilla estado : List.of(EstadoPlantilla.ACTIVA, EstadoPlantilla.INACTIVA)) {
            PlantillaHorario horario = conEstado(estado);

            assertThatThrownBy(() -> service.reanudar(instructor, horario.getCodigoPlantillaHorario()))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_12"));
        }
        verify(plantillaRepository, never()).save(any());
        verify(generacion, never()).generar(any());
    }

    @Test
    void reanudar_siAhoraSeCruzaConOtroActivo_lanzaErrAca04YSigueEnPausa() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.PAUSADA);
        PlantillaHorario otro = existente(1, "10:00", "14:00", HOY, null);
        otro.setId(5L);
        activos(otro);

        assertThatThrownBy(() -> service.reanudar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_04"));

        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.PAUSADA);
        verify(plantillaRepository, never()).save(any());
        verify(generacion, never()).generar(any());
    }

    @Test
    void reanudar_siLaGeneracionFalla_elHorarioYaQuedoActivo() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.PAUSADA);
        activos();
        when(generacion.generar(horario)).thenThrow(new IllegalStateException("la base no responde"));

        assertThatThrownBy(() -> service.reanudar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOf(IllegalStateException.class);

        // * La tarea diaria completa las franjas que falten
        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
        verify(plantillaRepository).save(horario);
    }

    @Test
    void reanudar_horarioInexistenteODeOtroInstructor_lanzaErrAca05() {
        UUID codigo = UUID.randomUUID();
        when(plantillaRepository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(codigo,
            instructor)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reanudar(instructor, codigo)).isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_05"));
    }

    // ---------------------------------------------------------------- desactivar

    @Test
    void desactivar_eliminaLasFranjasFuturasYAlFinalMarcaInactivo() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.ACTIVA);
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString())).thenReturn(9L);

        service.desactivar(instructor, horario.getCodigoPlantillaHorario());

        InOrder orden = inOrder(franjaRepository, plantillaRepository);
        orden.verify(franjaRepository).eliminarFuturasNoReservadas(99L, HOY, "09:30");
        orden.verify(plantillaRepository).save(horario);
        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.INACTIVA);
    }

    @Test
    void desactivar_desdePausado_tambienFunciona() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.PAUSADA);

        service.desactivar(instructor, horario.getCodigoPlantillaHorario());

        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.INACTIVA);
        verify(plantillaRepository).save(horario);
    }

    @Test
    void desactivar_siElBorradoFalla_conservaSuEstado() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.ACTIVA);
        when(franjaRepository.eliminarFuturasNoReservadas(anyLong(), any(), anyString()))
            .thenThrow(new IllegalStateException("la base no responde"));

        assertThatThrownBy(() -> service.desactivar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
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
        PlantillaHorario horario = conEstado(EstadoPlantilla.INACTIVA);

        assertThatThrownBy(() -> service.desactivar(instructor, horario.getCodigoPlantillaHorario()))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_06"));
        verify(franjaRepository, never()).eliminarFuturasNoReservadas(anyLong(), any(), anyString());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void desactivar_noGeneraFranjas() {
        PlantillaHorario horario = conEstado(EstadoPlantilla.ACTIVA);

        service.desactivar(instructor, horario.getCodigoPlantillaHorario());

        verify(generacion, never()).generar(any());
    }
}
