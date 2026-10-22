package com.drivehub.identidad;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

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
