package com.drivehub.pagos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.exception.GlobalExceptionHandler;

/**
 * Las rutas internas (CU44) dentro de la seguridad real: la credencial reemplaza al JWT en `/internal/**`, y todo lo
 * demás sigue pidiendo JWT.
 */
@WebMvcTest(RutasInternasSeguridadTest.RutasDePrueba.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RutasInternasSeguridadTest.RutasDePrueba.class})
@TestPropertySource(properties = "app.internal.credencial-academico=credencial-de-prueba")
class RutasInternasSeguridadTest {

    @RestController
    static class RutasDePrueba {

        @PostMapping("/internal/v1/prueba")
        String interna() {
            return "interna";
        }

        @GetMapping("/api/v1/privada")
        String privada() {
            return "privada";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rutaInterna_conLaCredencial_responde200_sinJwt() throws Exception {
        mockMvc.perform(post("/internal/v1/prueba").header("X-Service-Key", "credencial-de-prueba"))
            .andExpect(status().isOk())
            .andExpect(content().string("interna"));
    }

    @Test
    void rutaInterna_sinCredencial_responde401ConErrPag19() throws Exception {
        mockMvc.perform(post("/internal/v1/prueba"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_PAG_19"));
    }

    @Test
    void rutaInterna_conUnJwtPeroSinCredencial_tambienResponde401ConErrPag19() throws Exception {
        mockMvc.perform(post("/internal/v1/prueba").header(HttpHeaders.AUTHORIZATION, "Bearer cualquiera"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_PAG_19"));
    }

    @Test
    void rutaInterna_conCredencialIncorrecta_responde401() throws Exception {
        mockMvc.perform(post("/internal/v1/prueba").header("X-Service-Key", "otra"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_PAG_19"));
    }

    @Test
    void laCredencialNoAbreLasRutasPublicasDeLaApi_siguenPidiendoJwt() throws Exception {
        mockMvc.perform(get("/api/v1/privada").header("X-Service-Key", "credencial-de-prueba"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }
}
