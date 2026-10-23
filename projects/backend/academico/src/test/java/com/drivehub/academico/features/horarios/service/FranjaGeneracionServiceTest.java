package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.config.HorariosProperties;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;

@ExtendWith(MockitoExtension.class)
class FranjaGeneracionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 a las 00:00: ninguna franja de hoy está vencida
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 0, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    @Mock
    private FranjaRepository franjaRepository;
    @Mock
    private PlantillaHorarioRepository plantillaRepository;

    private FranjaGeneracionService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new FranjaGeneracionService(new GeneradorFranjas(), franjaRepository, plantillaRepository, reloj,
            new HorariosProperties(ZONA, 28, "0 0 2 * * *", true));
    }

    private PlantillaHorario lunesDeOchoADoce(LocalDate hasta) {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), "Lunes", List.of(1),
            "08:00", "12:00", 60, HOY, hasta);
        horario.inicializarCodigo();
        horario.setId(7L);
        return horario;
    }

    // ---------------------------------------------------------------- generar

    @Test
    void generar_guardaLasFranjasQueCalculaElGeneradorConLosDatosDelHorario() {
        PlantillaHorario horario = lunesDeOchoADoce(null);
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);

        int insertadas = service.generar(horario);

        ArgumentCaptor<Franja> franjas = ArgumentCaptor.forClass(Franja.class);
        verify(franjaRepository, times(16)).insertarSiNoExiste(franjas.capture());
        assertThat(insertadas).isEqualTo(16);
        Franja primera = franjas.getAllValues().get(0);
        assertThat(primera.getFecha()).isEqualTo(HOY);
        assertThat(primera.getHoraInicio()).isEqualTo("08:00");
        assertThat(primera.getHoraFin()).isEqualTo("09:00");
        assertThat(primera.getPlantillaId()).isEqualTo(7L);
        assertThat(primera.getInstructorCodigo()).isEqualTo(horario.getInstructorCodigo());
        assertThat(primera.getEscuelaCodigo()).isEqualTo(horario.getEscuelaCodigo());
    }

    @Test
    void generar_devuelveSoloLasRealmenteInsertadas() {
        PlantillaHorario horario = lunesDeOchoADoce(null);
        // * Las dos primeras ya existían
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(false, false, true);

        int insertadas = service.generar(horario);

        assertThat(insertadas).isEqualTo(14);
    }

    @Test
    void generar_horarioDeLunesYMiercoles_guardaLasFranjasDeAmbosDias() {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), "Entre semana",
            List.of(1, 3), "08:00", "12:00", 60, HOY, null);
        horario.inicializarCodigo();
        horario.setId(7L);
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);

        int insertadas = service.generar(horario);

        // * 4 franjas por día, 2 días por semana, 4 semanas
        assertThat(insertadas).isEqualTo(32);
        ArgumentCaptor<Franja> guardadas = ArgumentCaptor.forClass(Franja.class);
        verify(franjaRepository, times(32)).insertarSiNoExiste(guardadas.capture());
        assertThat(guardadas.getAllValues()).extracting(f -> f.getFecha().getDayOfWeek().getValue())
            .containsOnly(1, 3);
    }

    @Test
    void generar_horarioConVigenciaTerminada_noGuardaNada() {
        PlantillaHorario horario = lunesDeOchoADoce(HOY.minusDays(1));

        assertThat(service.generar(horario)).isZero();
        verify(franjaRepository, never()).insertarSiNoExiste(any());
    }

    // ---------------------------------------------------------------- extender

    private void unaPagina(PlantillaHorario... horarios) {
        when(plantillaRepository.findByEstadoAndDeletedAtIsNull(eq(EstadoPlantilla.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(horarios)));
    }

    @Test
    void extender_completaLasFranjasQueFaltanDeTodosLosHorariosActivos() {
        unaPagina(lunesDeOchoADoce(null), lunesDeOchoADoce(null));
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);

        ResumenExtension resumen = service.extenderHorariosActivos();

        assertThat(resumen).isEqualTo(new ResumenExtension(2, 32, 0));
    }

    @Test
    void extender_correrloDosVeces_laSegundaNoCreaNada() {
        unaPagina(lunesDeOchoADoce(null));
        // * La primera corrida inserta todo; la segunda encuentra todo ya creado
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);
        ResumenExtension primera = service.extenderHorariosActivos();
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(false);
        ResumenExtension segunda = service.extenderHorariosActivos();

        assertThat(primera.franjasCreadas()).isEqualTo(16);
        assertThat(segunda.franjasCreadas()).isZero();
        assertThat(segunda.horariosProcesados()).isEqualTo(1);
    }

    @Test
    void extender_saltaElHorarioDeVigenciaVencida() {
        unaPagina(lunesDeOchoADoce(HOY.minusDays(1)), lunesDeOchoADoce(null));
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);

        ResumenExtension resumen = service.extenderHorariosActivos();

        assertThat(resumen.horariosProcesados()).isEqualTo(1);
        assertThat(resumen.franjasCreadas()).isEqualTo(16);
    }

    @Test
    void extender_unHorarioQueFalla_noDetieneALosDemasYSeCuenta() {
        PlantillaHorario malo = lunesDeOchoADoce(null);
        PlantillaHorario bueno = lunesDeOchoADoce(null);
        bueno.setId(8L);
        unaPagina(malo, bueno);
        doThrow(new IllegalStateException("la base no responde")).when(franjaRepository).insertarSiNoExiste(
            argThat(f -> f != null && f.getPlantillaId() == 7L));
        when(franjaRepository.insertarSiNoExiste(
            argThat(f -> f != null && f.getPlantillaId() == 8L))).thenReturn(true);

        ResumenExtension resumen = service.extenderHorariosActivos();

        assertThat(resumen.horariosConError()).isEqualTo(1);
        assertThat(resumen.horariosProcesados()).isEqualTo(2);
        assertThat(resumen.franjasCreadas()).isEqualTo(16);
    }

    @Test
    void extender_sinHorariosActivos_devuelveTodoEnCero() {
        when(plantillaRepository.findByEstadoAndDeletedAtIsNull(eq(EstadoPlantilla.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.extenderHorariosActivos()).isEqualTo(new ResumenExtension(0, 0, 0));
        verify(franjaRepository, never()).insertarSiNoExiste(any());
    }

    @Test
    void extender_recorreTodasLasPaginas() {
        PlantillaHorario a = lunesDeOchoADoce(null);
        PlantillaHorario b = lunesDeOchoADoce(null);
        PlantillaHorario c = lunesDeOchoADoce(null);
        // * Tres horarios en páginas de dos: la primera dice que hay otra
        when(plantillaRepository.findByEstadoAndDeletedAtIsNull(eq(EstadoPlantilla.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(a, b), PageRequest.of(0, 2), 3))
            .thenReturn(new PageImpl<>(List.of(c), PageRequest.of(1, 2), 3));
        when(franjaRepository.insertarSiNoExiste(any())).thenReturn(true);

        ResumenExtension resumen = service.extenderHorariosActivos();

        assertThat(resumen.horariosProcesados()).isEqualTo(3);
        verify(plantillaRepository, times(2)).findByEstadoAndDeletedAtIsNull(eq(EstadoPlantilla.ACTIVA),
            any(Pageable.class));
    }
}
