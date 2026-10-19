package com.drivehub.identidad.infrastructure.config;

import static org.mockito.Mockito.when;
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
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;

/**
 * El resource server: rutas públicas, token ausente/inválido, roles y 403.
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

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rutaPrivada_sinToken_responde401ConErrSys04EnElSobre() throws Exception {
        mockMvc.perform(get("/api/v1/privada"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void rutaPrivada_conTokenInvalidoOVencido_responde401() throws Exception {
        when(jwtDecoder.decode("malo")).thenThrow(new BadJwtException("firma inválida"));

        mockMvc.perform(get("/api/v1/privada").header(HttpHeaders.AUTHORIZATION, "Bearer malo"))
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
                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void rolConPermiso_pasa() throws Exception {
        mockMvc.perform(get("/api/v1/solo-super-admin")
                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
            .andExpect(status().isOk());
    }

    @Test
    void rutasPublicas_siguenAbiertasSinToken() throws Exception {
        // * No existe controller para estas rutas en este slice: lo que importa es que no responden 401
        mockMvc.perform(get("/health")).andExpect(status().is4xxClientError()).andExpect(result ->
            org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        mockMvc.perform(get("/.well-known/jwks.json")).andExpect(result ->
            org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}
