package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokenOpacoAdapterTest {

    private final TokenOpacoAdapter adaptador = new TokenOpacoAdapter();

    @Test
    void generar_noSeRepiteYNoEsAdivinable() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(adaptador.generar());
        }

        assertThat(tokens).hasSize(1000);
        assertThat(tokens.iterator().next().length()).isGreaterThanOrEqualTo(43);
    }

    @Test
    void hashear_dael_Sha256En64CaracteresHexadecimalesYEsDeterministico() {
        String hash = adaptador.hashear("un-token");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+").isEqualTo(adaptador.hashear("un-token"));
        assertThat(hash).isNotEqualTo(adaptador.hashear("otro-token"));
    }
}
