package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.infrastructure.adapter.out.security.JwtEmisorAdapter;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

@WebMvcTest(JwksController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class JwksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtEmisorAdapter emisor;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void jwks_devuelveLaClavePublicaSinTokenYSinLaPrivada() throws Exception {
        RSAKey clave = new RSAKeyGenerator(2048).keyID("kid-de-prueba").keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256).generate();
        when(emisor.jwkSetPublico()).thenReturn(new JWKSet(clave.toPublicJWK()));

        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].kid").value("kid-de-prueba"))
            .andExpect(jsonPath("$.keys[0].n").exists())
            .andExpect(jsonPath("$.keys[0].d").doesNotExist())
            .andExpect(jsonPath("$.keys[0].p").doesNotExist())
            .andExpect(jsonPath("$.keys[0].q").doesNotExist());
    }
}
