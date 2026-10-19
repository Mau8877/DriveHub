package com.drivehub.identidad.infrastructure.adapter.out.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

/**
 * Par de claves RSA con el que Identidad firma los JWT (RS256).
 *
 * * La clave privada solo la tiene Identidad; los demás servicios descargan la
 * * pública de /.well-known/jwks.json para verificar, y por eso ninguno puede emitir tokens.
 * * Se lee de `app.auth.jwt.private-key-pem` / `public-key-pem` (PEM). Si no
 * * están y el perfil es `dev` (o `app.auth.jwt.generar-si-faltan=true`, que
 * * usan los tests), se genera un par en memoria (al reiniciar, las sesiones
 * * caen); en cualquier otro caso el servicio no arranca.
 * ! Las claves privadas nunca van al repositorio.
 */
@Configuration
public class ClavesJwt {

    private static final Logger LOG = LoggerFactory.getLogger(ClavesJwt.class);

    @Bean
    public RSAKey claveRsa(
            Environment entorno,
            @Value("${app.auth.jwt.private-key-pem:}") String privadaPem,
            @Value("${app.auth.jwt.public-key-pem:}") String publicaPem,
            @Value("${app.auth.jwt.generar-si-faltan:false}") boolean generarSiFaltan) throws Exception {
        if (!privadaPem.isBlank() && !publicaPem.isBlank()) {
            RSAPrivateKey privada = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decodificarPem(privadaPem)));
            RSAPublicKey publica = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decodificarPem(publicaPem)));
            return conHuella(new RSAKey.Builder(publica).privateKey(privada));
        }
        if (!generarSiFaltan && !entorno.acceptsProfiles(Profiles.of("dev"))) {
            throw new IllegalStateException(
                "Faltan JWT_PRIVATE_KEY_PEM y JWT_PUBLIC_KEY_PEM: fuera de dev son obligatorias");
        }
        LOG.warn("Sin claves JWT configuradas: se genera un par RSA en memoria (solo dev; al reiniciar caen las sesiones)");
        RSAKey generada = new RSAKeyGenerator(2048).generate();
        return conHuella(new RSAKey.Builder(generada.toRSAPublicKey()).privateKey(generada.toRSAPrivateKey()));
    }

    private RSAKey conHuella(RSAKey.Builder constructor) throws Exception {
        RSAKey sinId = constructor.keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build();
        return new RSAKey.Builder(sinId).keyID(sinId.computeThumbprint().toString()).build();
    }

    private byte[] decodificarPem(String pem) {
        String limpio = pem.replaceAll("-----(BEGIN|END)[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(limpio);
    }
}
