package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

class ClavesJwtTest {

    private final ClavesJwt configuracion = new ClavesJwt();

    @Test
    void sinClavesFueraDeDev_noArranca() {
        assertThatThrownBy(() -> configuracion.claveRsa(new MockEnvironment(), "", "", false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_PRIVATE_KEY_PEM");
    }

    @Test
    void sinClavesEnDev_generaUnParEnMemoriaConHuella() throws Exception {
        MockEnvironment dev = new MockEnvironment();
        dev.setActiveProfiles("dev");

        RSAKey clave = configuracion.claveRsa(dev, "", "", false);

        assertThat(clave.isPrivate()).isTrue();
        assertThat(clave.getKeyID()).isNotBlank();
    }

    @Test
    void conLaBanderaDeTests_generaUnPar() throws Exception {
        assertThat(configuracion.claveRsa(new MockEnvironment(), "", "", true).getKeyID()).isNotBlank();
    }

    @Test
    void conClavesPem_usaEsasClaves() throws Exception {
        RSAKey original = new RSAKeyGenerator(2048).generate();
        String privada = pem("PRIVATE KEY", original.toRSAPrivateKey().getEncoded());
        String publica = pem("PUBLIC KEY", original.toRSAPublicKey().getEncoded());

        RSAKey leida = configuracion.claveRsa(new MockEnvironment(), privada, publica, false);

        assertThat(leida.toRSAPublicKey()).isEqualTo(original.toRSAPublicKey());
        assertThat(leida.isPrivate()).isTrue();
    }

    private String pem(String tipo, byte[] der) {
        return "-----BEGIN " + tipo + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
            + "\n-----END " + tipo + "-----";
    }
}
