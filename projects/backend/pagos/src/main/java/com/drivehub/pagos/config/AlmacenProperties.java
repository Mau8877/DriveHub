package com.drivehub.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Dónde se guardan los PDF de los comprobantes (CU30), prefijo `app.pagos.almacen`.
 *
 * * `endpoint`: la dirección interna de MinIO (`MINIO_ENDPOINT`). `accessKey` y `secretKey` salen de variables de
 * * entorno (`MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`): pueden estar vacías (tests, desarrollo sin MinIO) y el servicio
 * * arranca igual; falla recién al guardar o leer un PDF. NUNCA se escriben en el repo.
 * * `bucket`: el bucket privado de los comprobantes (`MINIO_BUCKET_COMPROBANTES`), el de Pagos: `drivehub-pagos`.
 * * `timeoutSegundos`: cuánto se espera a MinIO antes de dar el servicio por caído.
 */
@Validated
@ConfigurationProperties(prefix = "app.pagos.almacen")
public record AlmacenProperties(
        @NotBlank @DefaultValue("http://localhost:9000") String endpoint,
        @DefaultValue("") String accessKey,
        @DefaultValue("") String secretKey,
        @NotBlank @DefaultValue("drivehub-pagos") String bucket,
        @Min(1) @DefaultValue("5") int timeoutSegundos) {
}
