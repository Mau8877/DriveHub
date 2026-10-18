package com.example.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica el contrato de CORS de SecurityConfig contra /health (el único
 * endpoint permitAll real que existe hoy -- no hay controllers de negocio
 * todavía para probar esto contra /api/v1/**). Via MockMvc, no un servidor
 * embebido real: pasa por el filter chain completo (incluida
 * RateLimitFilter y CORS) sin depender de un cliente HTTP externo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflight_desdeOrigenPermitido_devuelveAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void preflight_desdeOrigenNoPermitido_seRechazaSinCabecerasCors() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://origen-no-permitido.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void get_conOrigenPermitido_respondeConLaCabeceraCorsCorrespondiente() throws Exception {
        mockMvc.perform(get("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
