package com.drivehub.identidad.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConverterTest {

    private Jwt jwt(Object roles) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256").subject("u").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60));
        if (roles != null) {
            b.claim("roles", roles);
        }
        return b.build();
    }

    // * Spring Security 7 suma sus propias autoridades de factor (FACTOR_BEARER): solo interesan los ROLE_
    private List<String> roles(org.springframework.security.authentication.AbstractAuthenticationToken token) {
        return token.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).toList();
    }

    @Test
    void convertirElClaimRoles_daAutoridadesRolePrefijadas() {
        var token = new JwtRolesConverter().convert(jwt(List.of("ADMIN_ESCUELA", "INSTRUCTOR")));

        assertThat(roles(token)).containsExactlyInAnyOrder("ROLE_ADMIN_ESCUELA", "ROLE_INSTRUCTOR");
    }

    @Test
    void sinClaimRoles_noDaAutoridades() {
        assertThat(roles(new JwtRolesConverter().convert(jwt(null)))).isEmpty();
    }
}
