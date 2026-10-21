package com.drivehub.identidad.infrastructure.adapter.out.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Emite el token de acceso: JWT firmado con RS256.
 *
 * * Claims: iss, sub (codigo del usuario), roles, escuela (ausente para el
 * * Super admin), iat, exp y jti. El `kid` va en el encabezado para que los
 * * demás servicios elijan la clave del JWKS.
 */
@Component
public class JwtEmisorAdapter implements EmisorTokenAccesoPort {

    public static final String EMISOR = "drivehub-identidad";

    private final RSAKey claveRsa;
    private final RSASSASigner firmador;
    private final Duration accessTtl;

    public JwtEmisorAdapter(RSAKey claveRsa, @Value("${app.auth.access-ttl-minutos:15}") long accessTtlMinutos)
            throws JOSEException {
        this.claveRsa = claveRsa;
        this.firmador = new RSASSASigner(claveRsa);
        this.accessTtl = Duration.ofMinutes(accessTtlMinutos);
    }

    @Override
    public String emitir(Usuario usuario, Instant emitidoEn) {
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(EMISOR)
            .subject(usuario.getCodigo().toString())
            .claim("roles", roles)
            .issueTime(Date.from(emitidoEn))
            .expirationTime(Date.from(emitidoEn.plus(accessTtl)))
            .jwtID(UUID.randomUUID().toString());
        if (usuario.getEscuelaCodigo() != null) {
            claims.claim("escuela", usuario.getEscuelaCodigo().toString());
        }
        try {
            SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(claveRsa.getKeyID()).build(), claims.build());
            jwt.sign(firmador);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de acceso", e);
        }
    }

    /** Conjunto de claves públicas (JWKS) para que los demás servicios verifiquen los tokens. */
    public JWKSet jwkSetPublico() {
        return new JWKSet(claveRsa.toPublicJWK());
    }
}
