package com.drivehub.identidad.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio: resource server de JWT.
 *
 * * CORS y rate limit NO viven acá: el navegador nunca habla directo con
 * * un microservicio, todo entra por el API Gateway (projects/backend/gateway),
 * * que es quien aplica ambos.
 * * Cada servicio valida el token por su cuenta (no confía en que la petición
 * * venga del gateway). La decisión por rol vive en el caso de uso con
 * * @PreAuthorize (EnableMethodSecurity), no en el controller.
 * * Públicas: infraestructura, registro (CU01), login y renovación (CU02) y el JWKS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestAuthenticationHandlers manejadores = new RestAuthenticationHandlers();
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // * Claves públicas para verificar los JWT
                .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                // * Registro del estudiante (CU01): público, sin sesión
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                // * Inicio, renovación (CU02) y cierre de sesión (CU03)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                    "/api/v1/auth/logout").permitAll()
                // * Confirmar el correo (CU04b): el enlace se abre fuera de la sesión
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verificar-correo/confirmar").permitAll()
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
}
