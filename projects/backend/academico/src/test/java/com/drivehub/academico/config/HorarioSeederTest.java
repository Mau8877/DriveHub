package com.drivehub.academico.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;
import com.drivehub.academico.features.horarios.service.FranjaGeneracionService;

@ExtendWith(MockitoExtension.class)
class HorarioSeederTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneId.of("America/La_Paz"));

    @Mock
    private PlantillaHorarioRepository plantillaRepository;

    @Mock
    private FranjaGeneracionService generacion;

    private HorarioSeeder seeder;

    @BeforeEach
    void preparar() {
        seeder = new HorarioSeeder(plantillaRepository, generacion, RELOJ);
    }

    @Test
    void run_sinHorario_creaElDeLaEscuelaYElInstructorDeEjemploYGeneraSusFranjas() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(
            HorarioSeeder.CODIGO_INSTRUCTOR_EXAMPLE, List.of(EstadoPlantilla.values()))).thenReturn(List.of());
        when(plantillaRepository.save(any(PlantillaHorario.class))).thenAnswer(i -> i.getArgument(0));
        when(generacion.generar(any(PlantillaHorario.class))).thenReturn(112);

        seeder.run(null);

        ArgumentCaptor<PlantillaHorario> creado = ArgumentCaptor.forClass(PlantillaHorario.class);
        verify(plantillaRepository).save(creado.capture());
        PlantillaHorario horario = creado.getValue();
        assertThat(horario.getEscuelaCodigo()).isEqualTo(HorarioSeeder.CODIGO_ESCUELA_EXAMPLE);
        assertThat(horario.getInstructorCodigo()).isEqualTo(HorarioSeeder.CODIGO_INSTRUCTOR_EXAMPLE);
        assertThat(horario.getDiasSemana()).containsExactly(1, 2, 3, 4, 5);
        assertThat(horario.getHoraInicio()).isEqualTo("08:00");
        assertThat(horario.getHoraFin()).isEqualTo("12:00");
        assertThat(horario.getDuracionClaseMin()).isEqualTo(60);
        assertThat(horario.getVigenteDesde()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(horario.getVigenteHasta()).isNull();
        assertThat(horario.estaActiva()).isTrue();
        verify(generacion).generar(horario);
    }

    @Test
    void run_conUnHorarioEnCualquierEstado_noCreaNadaNiGeneraFranjas() {
        when(plantillaRepository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(
            HorarioSeeder.CODIGO_INSTRUCTOR_EXAMPLE, List.of(EstadoPlantilla.values())))
            .thenReturn(List.of(new PlantillaHorario(HorarioSeeder.CODIGO_ESCUELA_EXAMPLE,
                HorarioSeeder.CODIGO_INSTRUCTOR_EXAMPLE, "Otro", List.of(1), "09:00", "10:00", 60,
                LocalDate.of(2026, 10, 1), null)));

        seeder.run(null);

        verify(plantillaRepository, never()).save(any());
        verify(generacion, never()).generar(any());
    }

    @Test
    void elCodigoDelInstructorEsElQueSiembraIdentidad() {
        assertThat(HorarioSeeder.CODIGO_INSTRUCTOR_EXAMPLE.toString()).isEqualTo("0198f0a0-0000-7000-8000-000000000002");
        assertThat(HorarioSeeder.CODIGO_ESCUELA_EXAMPLE.toString()).isEqualTo("0198f0a0-0000-7000-8000-000000000001");
    }
}
