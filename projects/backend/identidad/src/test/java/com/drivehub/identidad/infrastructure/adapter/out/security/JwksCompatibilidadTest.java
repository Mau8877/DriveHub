package com.drivehub.identidad.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.sun.net.httpserver.HttpServer;

/**
 * El contrato con el resto de los servicios: un decodificador configurado
 * como el del gateway (JWKS por HTTP + emisor) acepta los tokens que emite
 * Identidad, y rechaza los que firma otra clave.
 */
class JwksCompatibilidadTest {

    private static HttpServer jwks;
    private static JwtEmisorAdapter emisor;
    private static NimbusJwtDecoder decodificadorComoElDelGateway;

    @BeforeAll
    static void levantarJwks() throws Exception {
        RSAKey clave = new RSAKeyGenerator(2048).keyID("kid-compat").generate();
        emisor = new JwtEmisorAdapter(clave, 15);
        jwks = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwks.createContext("/.well-known/jwks.json", intercambio -> {
            byte[] cuerpo = emisor.jwkSetPublico().toString().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        jwks.start();
        decodificadorComoElDelGateway = NimbusJwtDecoder
            .withJwkSetUri("http://localhost:" + jwks.getAddress().getPort() + "/.well-known/jwks.json").build();
        decodificadorComoElDelGateway.setJwtValidator(
            new DelegatingOAuth2TokenValidator<Jwt>(JwtValidators.createDefaultWithIssuer(JwtEmisorAdapter.EMISOR)));
    }

    @AfterAll
    static void apagar() {
        jwks.stop(0);
    }

    private Usuario usuario() {
        return Usuario.builder().codigo(UUID.randomUUID()).escuelaCodigo(UUID.randomUUID())
            .roles(Set.of(Rol.builder().nombre(RolNombres.ADMIN_ESCUELA).build())).build();
    }

    @Test
    void unTokenDeIdentidad_loAceptaUnDecodificadorQueSoloConoceElJwks() {
        Usuario u = usuario();

        Jwt jwt = decodificadorComoElDelGateway.decode(emisor.emitir(u, Instant.now()));

        assertThat(jwt.getSubject()).isEqualTo(u.getCodigo().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN_ESCUELA");
        assertThat(jwt.getClaimAsString("escuela")).isEqualTo(u.getEscuelaCodigo().toString());
    }

    @Test
    void unTokenFirmadoPorOtraClave_noLoAcepta() throws Exception {
        JwtEmisorAdapter impostor = new JwtEmisorAdapter(new RSAKeyGenerator(2048).keyID("kid-compat").generate(), 15);
        String token = impostor.emitir(usuario(), Instant.now());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> decodificadorComoElDelGateway.decode(token))
            .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void unTokenVencido_noLoAcepta() {
        String token = emisor.emitir(usuario(), Instant.now().minusSeconds(3600));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> decodificadorComoElDelGateway.decode(token))
            .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }
}
