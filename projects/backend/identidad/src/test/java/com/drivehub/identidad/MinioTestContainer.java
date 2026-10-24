package com.drivehub.identidad;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Un MinIO real compartido por los tests que lo necesitan (CU06). Misma imagen que docker-compose
 * (la oficial minio/minio ya no está en Docker Hub). Se levanta a mano y una sola vez; quien lo
 * usa antes comprueba que Docker esté disponible (patrón assumeTrue de TESTING_BACKEND.md).
 */
public final class MinioTestContainer {

    public static final String USUARIO = "minio_test";
    public static final String CLAVE = "minio_test_pass_123";
    public static final String BUCKET = "drivehub-identidad";

    private static MinIOContainer minio;

    private MinioTestContainer() {
    }

    /**
     * Crea el bucket de los tests si todavía no existe.
     *
     * * El MinIO es uno solo para todas las clases de test y el orden en que corren no está
     * * fijado: la primera lo crea y las demás lo encuentran creado (409), que no es un error.
     */
    public static void crearBucketSiFalta(S3Client s3) {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        } catch (BucketAlreadyOwnedByYouException e) {
            // * Ya lo creó otra clase de test en este mismo container
        }
    }

    public static synchronized MinIOContainer iniciar() {
        if (minio == null) {
            minio = new MinIOContainer(DockerImageName.parse("cgr.dev/chainguard/minio:latest")
                    .asCompatibleSubstituteFor("minio/minio"))
                .withUserName(USUARIO)
                .withPassword(CLAVE)
                .withCreateContainerCmdModifier(cmd -> cmd.withUser("0"));
            minio.start();
        }
        return minio;
    }
}
