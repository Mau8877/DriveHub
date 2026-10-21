package com.drivehub.academico.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.exception.GlobalExceptionHandler;

/**
 * El resource server: token ausente o inválido, roles y 403.
 */
@WebMvcTest(SecurityConfigTest.RutasDePrueba.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityConfigTest.RutasDePrueba.class})
class SecurityConfigTest {

    @RestController
    static class RutasDePrueba {

        @GetMapping("/api/v1/privada")
        String privada() {
            return "ok";
        }

        @GetMapping("/api/v1/solo-super-admin")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        String soloSuperAdmin() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rutaPrivada_sinToken_responde401ConErrSys04EnElSobre() throws Exception {
        mockMvc.perform(get("/api/v1/privada"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void rutaPrivada_conUnTokenQueNoEsUnJwt_responde401() throws Exception {
        mockMvc.perform(get("/api/v1/privada").header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void rutaPrivada_conTokenValido_pasa() throws Exception {
        mockMvc.perform(get("/api/v1/privada").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void rolSinPermiso_responde403ConErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/solo-super-admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void rolConPermiso_pasa() throws Exception {
        mockMvc.perform(get("/api/v1/solo-super-admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
            .andExpect(status().isOk());
    }
}
