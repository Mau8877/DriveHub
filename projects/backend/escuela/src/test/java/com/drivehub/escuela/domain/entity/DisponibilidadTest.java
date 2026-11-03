package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * CU11: cuándo un instructor está disponible (sin Spring).
 */
class DisponibilidadTest {

    @Test
    void de_activoConAlgunaLibre_esDisponible() {
        assertThat(Disponibilidad.de(EstadoInstructor.ACTIVO, 6, 1)).isEqualTo(Disponibilidad.DISPONIBLE);
        assertThat(Disponibilidad.de(EstadoInstructor.ACTIVO, 6, 6)).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    @Test
    void de_activoConFranjasPeroNingunaLibre_esCompleto() {
        assertThat(Disponibilidad.de(EstadoInstructor.ACTIVO, 4, 0)).isEqualTo(Disponibilidad.COMPLETO);
    }

    @Test
    void de_activoSinFranjas_esSinHorario() {
        assertThat(Disponibilidad.de(EstadoInstructor.ACTIVO, 0, 0)).isEqualTo(Disponibilidad.SIN_HORARIO);
    }

    @Test
    void de_deLicenciaOInactivo_noEstaDisponibleAunqueTengaFranjasLibres() {
        assertThat(Disponibilidad.de(EstadoInstructor.LICENCIA, 6, 3)).isEqualTo(Disponibilidad.NO_DISPONIBLE);
        assertThat(Disponibilidad.de(EstadoInstructor.INACTIVO, 6, 3)).isEqualTo(Disponibilidad.NO_DISPONIBLE);
        assertThat(Disponibilidad.de(EstadoInstructor.LICENCIA, 0, 0)).isEqualTo(Disponibilidad.NO_DISPONIBLE);
    }

    @Test
    void orden_losDisponiblesVanPrimeroYLosNoDisponiblesAlFinal() {
        assertThat(Disponibilidad.DISPONIBLE.orden()).isLessThan(Disponibilidad.COMPLETO.orden());
        assertThat(Disponibilidad.COMPLETO.orden()).isLessThan(Disponibilidad.SIN_HORARIO.orden());
        assertThat(Disponibilidad.SIN_HORARIO.orden()).isLessThan(Disponibilidad.NO_DISPONIBLE.orden());
    }
}
