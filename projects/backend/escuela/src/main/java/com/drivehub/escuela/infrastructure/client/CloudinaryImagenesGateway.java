package com.drivehub.escuela.infrastructure.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.escuela.domain.gateway.FirmaSubidaImagen;
import com.drivehub.escuela.domain.gateway.ImagenesGateway;

/**
 * Firma las subidas a Cloudinary: SHA-1 de los parámetros ordenados (`folder`, `timestamp`)
 * seguidos del API secret, que es el esquema de firma de su API de subida.
 *
 * ! El API secret solo se usa acá para firmar; nunca se escribe en respuestas ni en registros.
 */
@Component
public class CloudinaryImagenesGateway implements ImagenesGateway {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryImagenesGateway(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public FirmaSubidaImagen firmarSubida(String carpeta) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException(
                "Cloudinary no está configurado: faltan CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET");
        }
        long timestamp = Instant.now().getEpochSecond();
        String aFirmar = "folder=" + carpeta + "&timestamp=" + timestamp + apiSecret;
        return new FirmaSubidaImagen(cloudName, apiKey, timestamp, carpeta, sha1(aFirmar));
    }

    private static String sha1(String texto) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no disponible", e);
        }
    }
}
