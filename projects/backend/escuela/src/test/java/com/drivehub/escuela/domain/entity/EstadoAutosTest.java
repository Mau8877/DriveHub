package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * CU11: cómo están los autos de un instructor (sin Spring).
 */
class EstadoAutosTest {

    @Test
    void de_sinAutos_esSinAuto() {
        assertThat(EstadoAutos.de(List.of())).isEqualTo(EstadoAutos.SIN_AUTO);
        assertThat(EstadoAutos.de(null)).isEqualTo(EstadoAutos.SIN_AUTO);
    }

    @Test
    void de_conAlgunAutoDisponible_esDisponible() {
        assertThat(EstadoAutos.de(List.of(EstadoVehiculo.DISPONIBLE))).isEqualTo(EstadoAutos.DISPONIBLE);
        assertThat(EstadoAutos.de(List.of(EstadoVehiculo.MANTENIMIENTO, EstadoVehiculo.DISPONIBLE)))
            .isEqualTo(EstadoAutos.DISPONIBLE);
    }

    @Test
    void de_conAutosPeroNingunoDisponible_esNoDisponible() {
        assertThat(EstadoAutos.de(List.of(EstadoVehiculo.MANTENIMIENTO))).isEqualTo(EstadoAutos.NO_DISPONIBLE);
        assertThat(EstadoAutos.de(List.of(EstadoVehiculo.BAJA, EstadoVehiculo.MANTENIMIENTO)))
            .isEqualTo(EstadoAutos.NO_DISPONIBLE);
    }
}
