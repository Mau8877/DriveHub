package com.drivehub.escuela.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import com.drivehub.escuela.domain.gateway.FirmaSubidaImagen;

class CloudinaryImagenesGatewayTest {

    private static String sha1(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void firmarSubida_firmaCarpetaYHoraConElSecretYNoLoDevuelve() throws Exception {
        CloudinaryImagenesGateway gateway = new CloudinaryImagenesGateway("mi-nube", "123456", "secreto-de-prueba");

        FirmaSubidaImagen firma = gateway.firmarSubida("drivehub/e1/vehiculos");

        assertThat(firma.cloudName()).isEqualTo("mi-nube");
        assertThat(firma.apiKey()).isEqualTo("123456");
        assertThat(firma.carpeta()).isEqualTo("drivehub/e1/vehiculos");
        assertThat(firma.firma())
            .isEqualTo(sha1("folder=drivehub/e1/vehiculos&timestamp=" + firma.timestamp() + "secreto-de-prueba"));
        assertThat(firma.toString()).doesNotContain("secreto-de-prueba");
    }

    @Test
    void firmarSubida_sinConfiguracion_lanzaIllegalState() {
        assertThatThrownBy(() -> new CloudinaryImagenesGateway("", "", "").firmarSubida("x"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLOUDINARY_");
        assertThatThrownBy(() -> new CloudinaryImagenesGateway("nube", "key", " ").firmarSubida("x"))
            .isInstanceOf(IllegalStateException.class);
    }
}
