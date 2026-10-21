package com.drivehub.academico.features.horarios.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;

class PlantillaHorarioTest {

    @Test
    void constructor_horarioNuevo_quedaActivoYSinVehiculo() {
        UUID escuela = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();

        PlantillaHorario horario = new PlantillaHorario(escuela, instructor, 3, "08:00", "12:00", 60,
            LocalDate.of(2026, 10, 26), null);

        assertThat(horario.isActiva()).isTrue();
        assertThat(horario.getVehiculoCodigo()).isNull();
        assertThat(horario.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(horario.getInstructorCodigo()).isEqualTo(instructor);
        assertThat(horario.getDiaSemana()).isEqualTo(3);
        assertThat(horario.getVigenteHasta()).isNull();
    }

    @Test
    void inicializarCodigo_generaUuidV7YNoLoCambiaDespues() {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), 1, "08:00", "09:00",
            60, LocalDate.of(2026, 10, 26), null);

        horario.inicializarCodigo();
        UUID codigo = horario.getCodigoPlantillaHorario();
        horario.inicializarCodigo();

        assertThat(codigo.version()).isEqualTo(7);
        assertThat(horario.getCodigoPlantillaHorario()).isEqualTo(codigo);
    }

    @Test
    void franja_constructor_quedaLibreYSinVehiculoNiReserva() {
        UUID escuela = UUID.randomUUID();

        Franja franja = new Franja(escuela, 5L, UUID.randomUUID(), LocalDate.of(2026, 10, 26), "10:00", "11:00");

        assertThat(franja.getEstado()).isEqualTo(EstadoFranja.LIBRE);
        assertThat(franja.getVehiculoCodigo()).isNull();
        assertThat(franja.getReservaId()).isNull();
        assertThat(franja.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(franja.getPlantillaId()).isEqualTo(5L);
    }
}
