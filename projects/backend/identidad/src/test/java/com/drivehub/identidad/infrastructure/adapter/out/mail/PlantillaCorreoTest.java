package com.drivehub.identidad.infrastructure.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlantillaCorreoTest {

    private static final String ENLACE = "http://localhost:4200/account/verify-email?token=abc&x=1";

    @Test
    void armar_usaLaPaletaDeMarca() {
        String html = PlantillaCorreo.armar("Titulo", "Ana", List.of("Cuerpo"), "Abrir", ENLACE, null);

        assertThat(html).contains("#1E293B").contains("#F59E0B").contains("#F1F5F9")
            .contains("Drive").contains("Hub");
    }

    @Test
    void armar_conAccion_incluyeElBotonYElEnlaceEscapadoEnTexto() {
        String html = PlantillaCorreo.armar("Titulo", "Ana", List.of("Cuerpo"), "Abrir", ENLACE, "Nota");

        assertThat(html).contains(">Abrir</a>").contains("href=\"http://localhost:4200/account/verify-email?token=abc&amp;x=1\"")
            .contains("Si el botón no funciona").contains("Hola Ana,").contains("Nota");
    }

    @Test
    void armar_sinAccion_noTieneBotonNiEnlaces() {
        String html = PlantillaCorreo.armar("Aviso", "Ana", List.of("Cuerpo"), null, null, "Contacta a tu escuela");

        assertThat(html).doesNotContain("href").doesNotContain("http").doesNotContain("Si el botón no funciona");
    }

    @Test
    void armar_escapaTodoLoQueEscribeLaPersona() {
        String html = PlantillaCorreo.armar("Titulo", "<script>x</script>", List.of("<b>hola</b>"), "Abrir", ENLACE, null);

        assertThat(html).doesNotContain("<script>").doesNotContain("<b>hola</b>")
            .contains("&lt;script&gt;x&lt;/script&gt;").contains("&lt;b&gt;hola&lt;/b&gt;");
    }
}
