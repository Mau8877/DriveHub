package com.drivehub.pagos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio.
 *
 * * CORS y rate limit NO viven acá: el navegador nunca habla directo con
 * * un microservicio, todo entra por el API Gateway (projects/backend/gateway),
 * * que es quien aplica ambos.
 * TODO: validar el JWT emitido por Identidad (resource server) cuando se
 * TODO: implemente el login (CU02). Hasta entonces, cualquier ruta que no
 * TODO: sea de infraestructura queda cerrada (usuario en memoria de Spring).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
