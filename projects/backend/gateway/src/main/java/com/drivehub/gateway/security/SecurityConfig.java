package com.drivehub.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Primera barrera: ninguna petición privada llega a los servicios sin un JWT
 * válido de Identidad (CU02). Cada servicio lo vuelve a validar por su cuenta.
 *
 * * Las claves públicas se descargan de Identidad (JWKS): el gateway puede
 * * verificar tokens pero no emitirlos.
 * * Públicas: infraestructura, login y renovación, registro del estudiante y
 * * la consulta de la escuela por slug (un solo segmento). El resto de
 * * /api/v1/** exige token, incluida /api/v1/escuelas/por-codigo/*.
 * * El orden de filtros: CORS (máxima prioridad), rate limit (-101), seguridad (-100).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String EMISOR = "drivehub-identidad";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestAuthenticationHandlers manejadores = new RestAuthenticationHandlers();
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                    "/api/v1/auth/logout", "/api/v1/auth/verificar-correo/confirmar",
                    "/api/v1/auth/recuperar", "/api/v1/auth/restablecer").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/escuelas/*").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> { })
                .authenticationEntryPoint(manejadores)
                .accessDeniedHandler(manejadores)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(manejadores)
                .accessDeniedHandler(manejadores)
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.auth.jwks-uri}") String jwksUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(JwtValidators.createDefaultWithIssuer(EMISOR)));
        return decoder;
    }
}
