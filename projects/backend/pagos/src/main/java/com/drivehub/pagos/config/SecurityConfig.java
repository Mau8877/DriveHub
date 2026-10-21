package com.drivehub.pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio: resource server de JWT (CU02).
 *
 * * CORS y rate limit NO viven acá: el navegador nunca habla directo con
 * * un microservicio, todo entra por el API Gateway (projects/backend/gateway),
 * * que es quien aplica ambos.
 * * El servicio valida el token por su cuenta (no confía en que la petición
 * * venga del gateway) con las claves públicas de Identidad (JWKS). La decisión
 * * por rol vive en el caso de uso con @PreAuthorize (EnableMethodSecurity).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static final String EMISOR = "drivehub-identidad";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestAuthenticationHandlers manejadores = new RestAuthenticationHandlers();
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRolesConverter()))
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
