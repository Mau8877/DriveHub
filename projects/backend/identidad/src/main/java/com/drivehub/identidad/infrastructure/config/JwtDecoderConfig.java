package com.drivehub.identidad.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.drivehub.identidad.infrastructure.adapter.out.security.JwtEmisorAdapter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JOSEException;

/**
 * Identidad valida los tokens con su propia clave pública (no se pide el JWKS
 * a sí misma) y exige el emisor correcto y que no hayan vencido.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(RSAKey claveRsa) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(claveRsa.toRSAPublicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
            JwtValidators.createDefaultWithIssuer(JwtEmisorAdapter.EMISOR)));
        return decoder;
    }
}
