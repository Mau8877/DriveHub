package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeneradorFranjasTest {

    // * Lunes. Con 28 días contando hoy: lunes 26/10, 02/11, 09/11 y 16/11 (la ventana llega al 22/11)
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    private static final LocalDateTime LUNES_A_MEDIANOCHE = LUNES.atStartOfDay();
    private static final int HORIZONTE = 28;

    private final GeneradorFranjas generador = new GeneradorFranjas();

    private List<FranjaCalculada> lunesDeOchoADoce(LocalDate desde, LocalDate hasta, LocalDateTime ahora) {
        return generador.calcular(1, LocalTime.of(8, 0), LocalTime.of(12, 0), 60, desde, hasta, ahora, HORIZONTE);
    }

    @Test
    void calcular_lunesDeOchoADoceEnClasesDeUnaHora_generaCuatroFranjasPorLunes() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, null, LUNES_A_MEDIANOCHE);

        assertThat(franjas).hasSize(16);
        assertThat(franjas.stream().map(FranjaCalculada::fecha).distinct().toList())
            .containsExactly(LUNES, LUNES.plusDays(7), LUNES.plusDays(14), LUNES.plusDays(21));
        assertThat(franjas.subList(0, 4)).extracting(FranjaCalculada::horaInicio)
            .containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertThat(franjas.get(3).horaFin()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void calcular_ordenaPorFechaYLuegoPorHora() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, null, LUNES_A_MEDIANOCHE);

        assertThat(franjas).isSortedAccordingTo((a, b) -> {
            int porFecha = a.fecha().compareTo(b.fecha());
            return porFecha != 0 ? porFecha : a.horaInicio().compareTo(b.horaInicio());
        });
    }

    @Test
    void calcular_ventanaDeUnSoloBloque_generaUnaFranjaPorFecha() {
        List<FranjaCalculada> franjas = generador.calcular(1, LocalTime.of(8, 0), LocalTime.of(9, 0), 60, LUNES,
            null, LUNES_A_MEDIANOCHE, HORIZONTE);

        assertThat(franjas).hasSize(4);
    }

    @Test
    void calcular_elDiaDeHoyALaMitadDeLaManana_omiteLosBloquesYaIniciados() {
        // * 09:30: los de las 08:00 y las 09:00 ya empezaron; quedan 10:00 y 11:00 de hoy + 12 de los otros lunes
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, null, LUNES.atTime(9, 30));

        assertThat(franjas).hasSize(14);
        assertThat(franjas.stream().filter(f -> f.fecha().equals(LUNES)).map(FranjaCalculada::horaInicio))
            .containsExactly(LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void calcular_unBloqueQueEmpiezaExactamenteAhora_noSeGenera() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, null, LUNES.atTime(10, 0));

        assertThat(franjas.stream().filter(f -> f.fecha().equals(LUNES)).map(FranjaCalculada::horaInicio))
            .containsExactly(LocalTime.of(11, 0));
    }

    @Test
    void calcular_vigenteDesdeFuturo_noGeneraAntesDeEsaFecha() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES.plusDays(14), null, LUNES_A_MEDIANOCHE);

        assertThat(franjas).hasSize(8);
        assertThat(franjas.get(0).fecha()).isEqualTo(LUNES.plusDays(14));
    }

    @Test
    void calcular_vigenteHastaDentroDeLaVentana_cortaAhi() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, LUNES.plusDays(8), LUNES_A_MEDIANOCHE);

        assertThat(franjas).hasSize(8);
        assertThat(franjas.get(franjas.size() - 1).fecha()).isEqualTo(LUNES.plusDays(7));
    }

    @Test
    void calcular_vigenteHastaIgualAUnaFecha_laIncluye() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, LUNES.plusDays(7), LUNES_A_MEDIANOCHE);

        assertThat(franjas.stream().map(FranjaCalculada::fecha).distinct().toList())
            .containsExactly(LUNES, LUNES.plusDays(7));
    }

    @Test
    void calcular_vigenteHastaAnteriorAHoy_noGeneraNada() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES.minusDays(30), LUNES.minusDays(1),
            LUNES_A_MEDIANOCHE);

        assertThat(franjas).isEmpty();
    }

    @Test
    void calcular_vigenteHastaMasAllaDelHorizonte_loLimitaElHorizonte() {
        List<FranjaCalculada> franjas = lunesDeOchoADoce(LUNES, LUNES.plusYears(1), LUNES_A_MEDIANOCHE);

        assertThat(franjas).hasSize(16);
        assertThat(franjas.get(franjas.size() - 1).fecha()).isEqualTo(LUNES.plusDays(21));
    }

    @Test
    void calcular_cadaDiaDeLaSemanaApareceExactamenteCuatroVecesEnLaVentanaDeVeintiochoDias() {
        for (DayOfWeek dia : DayOfWeek.values()) {
            List<FranjaCalculada> franjas = generador.calcular(dia.getValue(), LocalTime.of(8, 0),
                LocalTime.of(9, 0), 60, LUNES, null, LUNES_A_MEDIANOCHE, HORIZONTE);

            assertThat(franjas).as("día %s", dia).hasSize(4);
            assertThat(franjas).allSatisfy(f -> assertThat(f.fecha().getDayOfWeek()).isEqualTo(dia));
        }
    }

    @Test
    void calcular_laVentanaNoDaLaVueltaAMedianoche() {
        List<FranjaCalculada> franjas = generador.calcular(1, LocalTime.of(22, 0), LocalTime.of(23, 59), 60, LUNES,
            null, LUNES_A_MEDIANOCHE, HORIZONTE);

        // * 22:00 a 23:00 entra; 23:00 a 24:00 no cabe antes de las 23:59
        assertThat(franjas).hasSize(4);
        assertThat(franjas).allSatisfy(f -> assertThat(f.horaInicio()).isEqualTo(LocalTime.of(22, 0)));
    }

    @Test
    void calcular_duracionNoPositiva_lanzaIllegalArgument() {
        assertThatThrownBy(() -> generador.calcular(1, LocalTime.of(8, 0), LocalTime.of(12, 0), 0, LUNES, null,
            LUNES_A_MEDIANOCHE, HORIZONTE)).isInstanceOf(IllegalArgumentException.class);
    }
}
