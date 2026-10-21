package com.drivehub.academico.features.horarios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CrearPlantillaHorarioRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CrearPlantillaHorarioRequest request(Integer dia, String inicio, String fin, Integer duracion) {
        return new CrearPlantillaHorarioRequest(dia, inicio, fin, duracion, null, null);
    }

    private Set<ConstraintViolation<CrearPlantillaHorarioRequest>> validar(CrearPlantillaHorarioRequest request) {
        return validator.validate(request);
    }

    @Test
    void valido_sinViolaciones() {
        assertThat(validar(request(1, "08:00", "12:00", 60))).isEmpty();
    }

    @Test
    void vigenciasOpcionales_nulasOPresentes_sonValidas() {
        var conVigencias = new CrearPlantillaHorarioRequest(7, "00:00", "23:59", 15, LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 1));

        assertThat(validar(conVigencias)).isEmpty();
        assertThat(validar(request(1, "08:00", "12:00", 60))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, -1, 100})
    void diaSemanaFueraDeUnoASiete_esInvalido(int dia) {
        assertThat(validar(request(dia, "08:00", "12:00", 60))).hasSize(1);
    }

    @Test
    void diaSemanaNulo_esInvalido() {
        assertThat(validar(request(null, "08:00", "12:00", 60))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"24:00", "9:00", "08:60", "8:5", "ocho", "08-00", "08:00:00"})
    void horaInicioConFormatoInvalido_esInvalida(String hora) {
        assertThat(validar(request(1, hora, "12:00", 60)))
            .anyMatch(v -> v.getPropertyPath().toString().equals("horaInicio"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"24:00", "9:00", "08:60", "ocho"})
    void horaFinConFormatoInvalido_esInvalida(String hora) {
        assertThat(validar(request(1, "08:00", hora, 60)))
            .anyMatch(v -> v.getPropertyPath().toString().equals("horaFin"));
    }

    @Test
    void horasVacias_sonInvalidas() {
        assertThat(validar(request(1, "", "", 60))).hasSizeGreaterThanOrEqualTo(2);
        assertThat(validar(request(1, null, null, 60))).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void duracionNula_esInvalida() {
        assertThat(validar(request(1, "08:00", "12:00", null))).hasSize(1);
    }

    @Test
    void duracionFueraDeRango_esDeNegocioYNoDeFormato() {
        // * 15 a 240 se valida en el service (ERR_ACA_02), no con anotaciones
        assertThat(validar(request(1, "08:00", "12:00", 5))).isEmpty();
        assertThat(validar(request(1, "08:00", "12:00", 1000))).isEmpty();
    }

    @Test
    void mensajesDeValidacion_estanEnEspanol() {
        var violaciones = validar(request(9, "9:00", "", null));

        assertThat(violaciones).extracting(ConstraintViolation::getMessage)
            .allSatisfy(m -> assertThat(m).matches("^(El|La) .*"));
    }
}
