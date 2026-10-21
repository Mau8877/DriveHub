package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherAdapterTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final BCryptPasswordHasherAdapter adaptador = new BCryptPasswordHasherAdapter(encoder);

    @Test
    void hashear_devuelveUnHashBCryptQueNoEsLaContrasenaYLaValida() {
        String hash = adaptador.hashear("123456789");

        assertThat(hash).isNotEqualTo("123456789").startsWith("$2");
        assertThat(encoder.matches("123456789", hash)).isTrue();
    }

    @Test
    void coincide_aciertaConLaContrasenaCorrectaYFallaConOtra() {
        String hash = adaptador.hashear("123456789");

        assertThat(adaptador.coincide("123456789", hash)).isTrue();
        assertThat(adaptador.coincide("987654321", hash)).isFalse();
    }
}
