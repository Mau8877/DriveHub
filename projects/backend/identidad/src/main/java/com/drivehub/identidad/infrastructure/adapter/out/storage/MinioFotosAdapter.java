package com.drivehub.identidad.infrastructure.adapter.out.storage;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Fotos de perfil en el bucket `drivehub-identidad` de MinIO (CU06).
 *
 * * El bucket es privado: la lectura es siempre por una URL prefirmada.
 * ! Nunca se loguean las credenciales ni la URL prefirmada: quien la tiene puede leer el archivo.
 */
@Component
public class MinioFotosAdapter implements AlmacenFotosPort {

    private static final Logger LOG = LoggerFactory.getLogger(MinioFotosAdapter.class);
    private static final String MODULO = "perfil";

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration ttlUrl;

    public MinioFotosAdapter(
            S3Client s3,
            S3Presigner presigner,
            @Value("${app.minio.bucket-name}") String bucket,
            @Value("${app.perfil.foto.url-ttl-minutos:60}") long ttlMinutos) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.ttlUrl = Duration.ofMinutes(ttlMinutos);
    }

    @Override
    public void guardar(String clave, byte[] contenido, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(clave).contentType(contentType).build(),
                RequestBody.fromBytes(contenido));
        } catch (SdkException e) {
            LOG.error("No se pudo guardar la foto en el almacenamiento", kv("module", MODULO), e);
            throw e;
        }
    }

    @Override
    public void borrar(String clave) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
        } catch (SdkException e) {
            LOG.error("No se pudo borrar la foto del almacenamiento", kv("module", MODULO), e);
            throw e;
        }
    }

    @Override
    public String urlDeLectura(String clave) {
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(ttlUrl)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(clave).build())
                    .build())
                .url().toString();
        } catch (SdkException e) {
            LOG.error("No se pudo firmar la URL de la foto", kv("module", MODULO), e);
            throw e;
        }
    }
}
