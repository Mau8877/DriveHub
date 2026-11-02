package com.drivehub.academico.features.clases.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.clases.enums.TipoPrueba;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EvaluarClaseRequestTest {

    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        validador = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static List<PuntajeDeCriterio> puntajes() {
        return List.of(new PuntajeDeCriterio("espejos", 8.0));
    }

    private static Set<ConstraintViolation<EvaluarClaseRequest>> validar(EvaluarClaseRequest request) {
        return validador.validate(request);
    }

    @Test
    void cuerpoValido_noTieneViolaciones() {
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO, puntajes(), "Bien")))
            .isEmpty();
    }

    @Test
    void sinClaseTipoOCriterios_esInvalido() {
        assertThat(validar(new EvaluarClaseRequest(null, TipoPrueba.MANEJO, puntajes(), null))).hasSize(1);
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), null, puntajes(), null))).hasSize(1);
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO, null, null))).hasSize(1);
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO, List.of(), null)))
            .hasSize(1);
    }

    @Test
    void criterioSinCodigoOSinPuntaje_esInvalido() {
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO,
            List.of(new PuntajeDeCriterio(" ", 5.0)), null))).hasSize(1);
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO,
            List.of(new PuntajeDeCriterio("espejos", null)), null))).hasSize(1);
    }

    @Test
    void observacionesDe500SonValidasYDe501No() {
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO, puntajes(),
            "a".repeat(500)))).isEmpty();
        assertThat(validar(new EvaluarClaseRequest(UUID.randomUUID(), TipoPrueba.MANEJO, puntajes(),
            "a".repeat(501)))).hasSize(1);
    }
}
