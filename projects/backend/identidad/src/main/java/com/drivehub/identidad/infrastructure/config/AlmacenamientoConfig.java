package com.drivehub.identidad.infrastructure.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Clientes de S3 contra MinIO (CU06).
 *
 * * Son dos endpoints distintos a propósito: el cliente escribe y borra por la red interna
 * * (`app.minio.url`), y el firmador usa el host que alcanza el navegador o el teléfono
 * * (`app.minio.public-url`), porque la firma de una URL incluye el host.
 * * MinIO no distingue regiones: se fija una y se usa path-style (`host/bucket/clave`).
 */
@Configuration
public class AlmacenamientoConfig {

    private static final Region REGION = Region.US_EAST_1;

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            @Value("${app.minio.url}") String url,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey) {
        return S3Client.builder()
            .endpointOverride(URI.create(url))
            .region(REGION)
            .credentialsProvider(credenciales(accessKey, secretKey))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            // * MinIO no necesita el checksum que el SDK agrega por defecto en cada subida
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${app.minio.public-url}") String urlPublica,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(urlPublica))
            .region(REGION)
            .credentialsProvider(credenciales(accessKey, secretKey))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private static StaticCredentialsProvider credenciales(String accessKey, String secretKey) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
