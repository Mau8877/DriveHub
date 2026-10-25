package com.drivehub.pagos.features.comprobantes.almacen;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.URI;
import java.time.Duration;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.AlmacenProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Guarda y lee los PDF de los comprobantes (CU30) en el bucket privado de MinIO, con el cliente S3.
 *
 * * Clave del objeto: `comprobantes/{escuelaCodigo}/{yyyy}/{numero}.pdf` (la arma quien guarda).
 * * Cualquier falla del almacén (red, MinIO caído, bucket inexistente, credenciales) es 503 `ERR_PAG_15`: no se
 * * crea el bucket en silencio, se avisa. Solo "no hay nada bajo esa clave" es `NoSuchElementException`.
 * ! Nunca se registra el PDF ni las credenciales.
 */
@Component
public class MinioAlmacenComprobantes implements AlmacenComprobantes {

    private static final Logger LOG = LoggerFactory.getLogger(MinioAlmacenComprobantes.class);
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos guardar o leer el comprobante. Inténtalo de nuevo más tarde.";

    private final AlmacenProperties propiedades;
    private final String bucket;
    private volatile S3Client clienteS3;

    public MinioAlmacenComprobantes(AlmacenProperties propiedades) {
        this.propiedades = propiedades;
        this.bucket = propiedades.bucket();
    }

    /**
     * El cliente S3, creado al primer uso: sin credenciales configuradas (tests, desarrollo sin MinIO) el servicio
     * arranca igual y recién al guardar o leer un PDF responde `ERR_PAG_15`.
     */
    private S3Client cliente() {
        S3Client actual = clienteS3;
        if (actual == null) {
            synchronized (this) {
                actual = clienteS3;
                if (actual == null) {
                    actual = crearCliente();
                    clienteS3 = actual;
                }
            }
        }
        return actual;
    }

    private S3Client crearCliente() {
        if (propiedades.accessKey().isBlank() || propiedades.secretKey().isBlank()) {
            LOG.warn("No hay credenciales del almacén de comprobantes configuradas", kv("module", "comprobantes"));
            throw noDisponible();
        }
        Duration espera = Duration.ofSeconds(propiedades.timeoutSegundos());
        return S3Client.builder()
            .endpointOverride(URI.create(propiedades.endpoint()))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(propiedades.accessKey(), propiedades.secretKey())))
            // * MinIO responde en `endpoint/bucket/clave`, no en `bucket.endpoint`
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            // * Sin sumas de verificación al final del cuerpo: MinIO las acepta, pero así el cuerpo viaja tal cual
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .apiCallTimeout(espera.multipliedBy(2)).apiCallAttemptTimeout(espera).build())
            .build();
    }

    @Override
    public void guardar(String clave, byte[] pdf) {
        try {
            cliente().putObject(PutObjectRequest.builder().bucket(bucket).key(clave).contentType("application/pdf")
                .build(), RequestBody.fromBytes(pdf));
        } catch (SdkException e) {
            LOG.warn("No se pudo guardar un comprobante en el almacén (tipo {})", e.getClass().getSimpleName(),
                kv("module", "comprobantes"));
            throw noDisponible();
        }
    }

    @Override
    public byte[] leer(String clave) {
        try {
            return cliente().getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(clave).build())
                .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new NoSuchElementException("No hay un comprobante guardado con esa clave");
        } catch (SdkException e) {
            LOG.warn("No se pudo leer un comprobante del almacén (tipo {})", e.getClass().getSimpleName(),
                kv("module", "comprobantes"));
            throw noDisponible();
        }
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE,
            MENSAJE_NO_DISPONIBLE);
    }
}
