package com.drivehub.escuela.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoVehiculoTest {

    @Test
    void permiteIrA_desdeDisponible_aMantenimientoYBaja() {
        assertThat(EstadoVehiculo.DISPONIBLE.permiteIrA(EstadoVehiculo.MANTENIMIENTO)).isTrue();
        assertThat(EstadoVehiculo.DISPONIBLE.permiteIrA(EstadoVehiculo.BAJA)).isTrue();
        assertThat(EstadoVehiculo.DISPONIBLE.permiteIrA(EstadoVehiculo.DISPONIBLE)).isFalse();
    }

    @Test
    void permiteIrA_desdeMantenimiento_aDisponibleYBaja() {
        assertThat(EstadoVehiculo.MANTENIMIENTO.permiteIrA(EstadoVehiculo.DISPONIBLE)).isTrue();
        assertThat(EstadoVehiculo.MANTENIMIENTO.permiteIrA(EstadoVehiculo.BAJA)).isTrue();
        assertThat(EstadoVehiculo.MANTENIMIENTO.permiteIrA(EstadoVehiculo.MANTENIMIENTO)).isFalse();
    }

    @Test
    void permiteIrA_desdeBaja_soloSeReactivaADisponible() {
        assertThat(EstadoVehiculo.BAJA.permiteIrA(EstadoVehiculo.DISPONIBLE)).isTrue();
        assertThat(EstadoVehiculo.BAJA.permiteIrA(EstadoVehiculo.MANTENIMIENTO)).isFalse();
        assertThat(EstadoVehiculo.BAJA.permiteIrA(EstadoVehiculo.BAJA)).isFalse();
    }
}
