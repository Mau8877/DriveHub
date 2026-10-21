package com.drivehub.academico.features.horarios.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;

class PlantillaHorarioTest {

    @Test
    void constructor_horarioNuevo_quedaActivoYSinVehiculo() {
        UUID escuela = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();

        PlantillaHorario horario = new PlantillaHorario(escuela, instructor, "Miércoles", List.of(3), "08:00",
            "12:00", 60, LocalDate.of(2026, 10, 26), null);

        assertThat(horario.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
        assertThat(horario.estaActiva()).isTrue();
        assertThat(horario.getVehiculoCodigo()).isNull();
        assertThat(horario.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(horario.getInstructorCodigo()).isEqualTo(instructor);
        assertThat(horario.getNombre()).isEqualTo("Miércoles");
        assertThat(horario.getDiasSemana()).containsExactly(3);
        assertThat(horario.getVigenteHasta()).isNull();
    }

    @Test
    void constructor_diasRepetidosYDesordenados_seGuardanSinRepetirYOrdenados() {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), "Mixto",
            List.of(5, 1, 5, 3), "08:00", "12:00", 60, LocalDate.of(2026, 10, 26), null);

        assertThat(horario.getDiasSemana()).containsExactly(1, 3, 5);
    }

    @Test
    void estaActiva_pausadaOInactiva_esFalso() {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), "x", List.of(1),
            "08:00", "09:00", 60, LocalDate.of(2026, 10, 26), null);

        horario.setEstado(EstadoPlantilla.PAUSADA);
        assertThat(horario.estaActiva()).isFalse();
        horario.setEstado(EstadoPlantilla.INACTIVA);
        assertThat(horario.estaActiva()).isFalse();
    }

    @Test
    void inicializarCodigo_generaUuidV7YNoLoCambiaDespues() {
        PlantillaHorario horario = new PlantillaHorario(UUID.randomUUID(), UUID.randomUUID(), "Lunes", List.of(1),
            "08:00", "09:00", 60, LocalDate.of(2026, 10, 26), null);

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
