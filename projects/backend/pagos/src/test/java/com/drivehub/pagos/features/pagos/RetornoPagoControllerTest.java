package com.drivehub.pagos.features.pagos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.pagos.controller.RetornoPagoController;

/**
 * La página a la que vuelve el estudiante de la pasarela de pago (CU26): pública, fija y sin reflejar nada.
 */
@WebMvcTest(RetornoPagoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class,
    RetornoPagoControllerTest.Configuracion.class})
class RetornoPagoControllerTest {

    /** El enlace a la app viene de la configuración (`PAGOS_ENLACE_APP`): acá, uno con caracteres a escapar. */
    static final String ENLACE_CON_AMPERSAND = "otraapp://app/volver?a=1&b=2";

    @TestConfiguration
    static class Configuracion {

        @Bean
        PagosProperties propiedades() {
            return new PagosProperties(new java.math.BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
                new PagosProperties.Pasarela("", "", 30, PagosPropertiesPrueba.URL_RETORNO, ENLACE_CON_AMPERSAND, "", 300),
                new PagosProperties.Vencimiento(300, 10), PagosPropertiesPrueba.RETENCION_REEMBOLSO);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinSesion_respondeLaPaginaHtml() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/retorno"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("Vuelve a DriveHub")))
            .andExpect(content().string(containsString("href=\"otraapp://app/volver?a=1&amp;b=2\"")))
            .andExpect(content().string(not(containsString("drivehub://"))));
    }

    @Test
    void laPaginaEsEnEspanolYSeDibujaBienEnUnTelefono() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/retorno"))
            .andExpect(content().string(containsString("<html lang=\"es\">")))
            .andExpect(content().string(containsString("charset=\"utf-8\"")))
            .andExpect(content().string(containsString("viewport")));
    }

    @Test
    void noReflejaNingunParametroDeLaDireccion() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/retorno").param("x", "<script>alert(1)</script>")
                .param("session_id", "cs_test_123"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<script>alert"))))
            .andExpect(content().string(not(containsString("cs_test_123"))));
    }

    @Test
    void noSeGuardaEnCache_yTieneUnaPoliticaDeContenidoQueNoPermiteScripts() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/retorno"))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));
    }

    @Test
    void soloSeAtiendeConGet_yLoDemasSigueExigiendoSesion() throws Exception {
        mockMvc.perform(post("/api/v1/pagos/retorno")).andExpect(status().isUnauthorized());
    }
}
