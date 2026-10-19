package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class JwtEmisorAdapterTest {

    private static RSAKey clave;
    private static JwtEmisorAdapter emisor;

    @BeforeAll
    static void crearEmisor() throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("kid-de-prueba").generate();
        emisor = new JwtEmisorAdapter(clave, 15);
    }

    private Usuario usuario(UUID escuela, String... roles) {
        return Usuario.builder().codigo(UUID.randomUUID()).escuelaCodigo(escuela)
            .roles(java.util.Arrays.stream(roles).map(r -> Rol.builder().nombre(r).build()).collect(java.util.stream.Collectors.toSet()))
            .build();
    }

    @Test
    void emitir_firmaConRs256YLaClavePublicaLoVerificaConLosClaimsEsperados() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario u = usuario(escuela, RolNombres.ADMIN_ESCUELA);
        Instant ahora = Instant.now();

        SignedJWT jwt = SignedJWT.parse(emisor.emitir(u, ahora));

        assertThat(jwt.verify(new RSASSAVerifier(clave.toRSAPublicKey()))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("kid-de-prueba");
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("drivehub-identidad");
        assertThat(claims.getSubject()).isEqualTo(u.getCodigo().toString());
        assertThat(claims.getStringListClaim("roles")).containsExactly("ADMIN_ESCUELA");
        assertThat(claims.getStringClaim("escuela")).isEqualTo(escuela.toString());
        assertThat(claims.getJWTID()).isNotBlank();
    }

    @Test
    void emitir_venceALos15MinutosYElSuperAdminNoLlevaElClaimEscuela() throws Exception {
        Usuario superAdmin = usuario(null, RolNombres.SUPER_ADMIN);
        Instant ahora = Instant.parse("2026-10-19T12:00:00Z");

        JWTClaimsSet claims = SignedJWT.parse(emisor.emitir(superAdmin, ahora)).getJWTClaimsSet();

        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(ahora.plus(Duration.ofMinutes(15)));
        assertThat(claims.getClaim("escuela")).isNull();
        assertThat(claims.getStringListClaim("roles")).isEqualTo(List.of("SUPER_ADMIN"));
    }

    @Test
    void jwkSetPublico_noContieneLaClavePrivada() {
        var json = emisor.jwkSetPublico().toJSONObject(true).toString();

        assertThat(json).contains("kid-de-prueba").doesNotContain("\"d\"").doesNotContain("\"p\"");
        assertThat(Set.of(emisor.jwkSetPublico().getKeys().get(0).isPrivate())).containsExactly(false);
    }
}
