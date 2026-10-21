package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class EscuelaTest {

    @Test
    void sinFechaDeEliminacion_estaActiva() {
        Escuela escuela = Escuela.builder().slug("a").nombre("A").build();

        assertThat(escuela.activa()).isTrue();
        assertThat(escuela.estado()).isEqualTo(EstadoEscuela.ACTIVA);
    }

    @Test
    void conFechaDeEliminacion_estaInactiva() {
        Escuela escuela = Escuela.builder().slug("a").nombre("A").eliminadaEn(Instant.now()).build();

        assertThat(escuela.activa()).isFalse();
        assertThat(escuela.estado()).isEqualTo(EstadoEscuela.INACTIVA);
    }
}
