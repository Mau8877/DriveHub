package com.drivehub.academico.features.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.config.ReservasProperties;
import com.drivehub.academico.features.reservas.model.Reserva;

class PlazoDeClaseTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * 2026-10-26 08:00 en La Paz
    private static final Instant AHORA = Instant.parse("2026-10-26T12:00:00Z");

    private final Clock reloj = Clock.fixed(AHORA, ZONA);
    private final PlazoDeClase plazo = new PlazoDeClase(new ReservasProperties(1, 10, 2, 12), reloj);

    private static Reserva reserva(LocalDate fecha, String inicio) {
        return new Reserva(UUID.randomUUID(), UUID.randomUUID(), 1L, fecha, inicio, "23:59", 1, AHORA);
    }

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Test
    void aTiempo_justoEnElLimiteDeDoceHoras_cuentaComoATiempo() {
        // * ahora 08:00, la clase es mañana a las 08:00 = 24 h; y hoy a las 20:00 = exactamente 12 h
        assertThat(plazo.aTiempo(reserva(HOY, "20:00"))).isTrue();
    }

    @Test
    void aTiempo_unMinutoAntesDelLimite_esTardia() {
        assertThat(plazo.aTiempo(reserva(HOY, "19:59"))).isFalse();
    }

    @Test
    void aTiempo_unaClaseDeOtroDiaMuyAdelante_esATiempo() {
        assertThat(plazo.aTiempo(reserva(HOY.plusDays(3), "07:00"))).isTrue();
    }

    @Test
    void aTiempo_unaClaseQueYaEmpezo_nuncaEstaATiempo() {
        assertThat(plazo.aTiempo(reserva(HOY, "07:00"))).isFalse();
        assertThat(plazo.aTiempo(reserva(HOY.minusDays(1), "09:00"))).isFalse();
    }

    @Test
    void haEmpezado_laClaseQueEmpiezaJustoAhoraYaEmpezo() {
        assertThat(plazo.haEmpezado(reserva(HOY, "08:00"))).isTrue();
        assertThat(plazo.haEmpezado(reserva(HOY, "07:59"))).isTrue();
        assertThat(plazo.haEmpezado(reserva(HOY, "08:01"))).isFalse();
        assertThat(plazo.haEmpezado(reserva(HOY.plusDays(1), "06:00"))).isFalse();
    }

    @Test
    void cambiarLaPropiedad_cambiaElResultado() {
        PlazoDeClase conDosHoras = new PlazoDeClase(new ReservasProperties(1, 10, 2, 2), reloj);
        PlazoDeClase conVeinticuatro = new PlazoDeClase(new ReservasProperties(1, 10, 2, 24), reloj);
        Reserva clase = reserva(HOY, "12:00");

        assertThat(conDosHoras.aTiempo(clase)).isTrue();
        assertThat(plazo.aTiempo(clase)).isFalse();
        assertThat(conVeinticuatro.aTiempo(clase)).isFalse();
    }
}
