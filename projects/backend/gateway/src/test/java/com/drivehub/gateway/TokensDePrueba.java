package com.drivehub.gateway;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Hace de Identidad en los tests: firma tokens con una clave de prueba y
 * publica su JWKS, que el gateway descarga para verificarlos.
 */
final class TokensDePrueba {

    private static final RSAKey CLAVE = generar("kid-de-prueba");
    private static final RSAKey OTRA_CLAVE = generar("kid-de-otro-emisor");

    private TokensDePrueba() {
    }

    private static RSAKey generar(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    static String jwks() {
        return new JWKSet(CLAVE.toPublicJWK()).toString();
    }

    static String valido() {
        return firmar(CLAVE, "drivehub-identidad", Instant.now().plusSeconds(600));
    }

    static String vencido() {
        return firmar(CLAVE, "drivehub-identidad", Instant.now().minusSeconds(60));
    }

    static String deOtroEmisor() {
        return firmar(CLAVE, "otro-emisor", Instant.now().plusSeconds(600));
    }

    static String conOtraClave() {
        return firmar(OTRA_CLAVE, "drivehub-identidad", Instant.now().plusSeconds(600));
    }

    private static String firmar(RSAKey clave, String emisor, Instant vence) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(emisor).subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN_ESCUELA")).issueTime(new Date())
                .expirationTime(Date.from(vence)).jwtID(UUID.randomUUID().toString()).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(clave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
