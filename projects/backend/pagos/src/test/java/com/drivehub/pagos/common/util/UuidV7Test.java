package com.drivehub.pagos.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void generar_devuelveUuidVersion7ConVarianteRfc() {
        UUID uuid = UuidV7.generar();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generar_enMilisegundosDistintos_quedaOrdenadoPorTiempo() throws InterruptedException {
        UUID primero = UuidV7.generar();
        Thread.sleep(2);
        UUID segundo = UuidV7.generar();

        assertThat(primero.toString()).isLessThan(segundo.toString());
    }
}
