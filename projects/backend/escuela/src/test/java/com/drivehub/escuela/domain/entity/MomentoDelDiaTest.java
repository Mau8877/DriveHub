package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * CU11: los tramos del día, con el mismo criterio que la reserva de clases (sin Spring).
 */
class MomentoDelDiaTest {

    @Test
    void manana_esAntesDeLas12() {
        assertThat(MomentoDelDia.MANANA.horaDesde()).isNull();
        assertThat(MomentoDelDia.MANANA.horaHasta()).isEqualTo("12:00");
    }

    @Test
    void tarde_vaDeLas12AAntesDeLas18() {
        assertThat(MomentoDelDia.TARDE.horaDesde()).isEqualTo("12:00");
        assertThat(MomentoDelDia.TARDE.horaHasta()).isEqualTo("18:00");
    }

    @Test
    void noche_esDesdeLas18() {
        assertThat(MomentoDelDia.NOCHE.horaDesde()).isEqualTo("18:00");
        assertThat(MomentoDelDia.NOCHE.horaHasta()).isNull();
    }

    @Test
    void lostramosNoSeSolapan_elFinDeUnoEsElInicioDelSiguiente() {
        assertThat(MomentoDelDia.MANANA.horaHasta()).isEqualTo(MomentoDelDia.TARDE.horaDesde());
        assertThat(MomentoDelDia.TARDE.horaHasta()).isEqualTo(MomentoDelDia.NOCHE.horaDesde());
    }
}
