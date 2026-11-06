package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HasheadorWhatsappAdapterTest {

    private final HasheadorWhatsappAdapter hasheador = new HasheadorWhatsappAdapter("clave-de-prueba");

    @Test
    void hashearTelefono_dejaElMismoHashSinImportarElFormatoDelNumero() {
        String base = hasheador.hashearTelefono("59171234567");

        assertThat(base).hasSize(64);
        assertThat(hasheador.hashearTelefono("+591 7123-4567")).isEqualTo(base);
        assertThat(hasheador.hashearTelefono("59171234568")).isNotEqualTo(base);
    }

    @Test
    void hashear_conOtraClave_daOtroHash() {
        HasheadorWhatsappAdapter otro = new HasheadorWhatsappAdapter("otra-clave");

        assertThat(otro.hashearTelefono("59171234567")).isNotEqualTo(hasheador.hashearTelefono("59171234567"));
        assertThat(otro.hashearCodigo("123456")).isNotEqualTo(hasheador.hashearCodigo("123456"));
    }

    @Test
    void hashear_unNumeroYUnCodigoIgualesNoSeConfunden() {
        assertThat(hasheador.hashearTelefono("123456")).isNotEqualTo(hasheador.hashearCodigo("123456"));
        assertThat(hasheador.hashearCorreo("123456")).isNotEqualTo(hasheador.hashearCodigo("123456"));
    }

    @Test
    void constructor_sinClave_falla() {
        assertThatThrownBy(() -> new HasheadorWhatsappAdapter(" ")).isInstanceOf(IllegalStateException.class);
    }
}
