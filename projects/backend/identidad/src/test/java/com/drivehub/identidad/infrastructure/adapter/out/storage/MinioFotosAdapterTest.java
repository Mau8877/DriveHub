package com.drivehub.identidad.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MinIOContainer;

import com.drivehub.identidad.MinioTestContainer;
import com.drivehub.identidad.infrastructure.config.AlmacenamientoConfig;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * El adaptador contra un MinIO real: guardar, firmar la lectura y borrar.
 */
class MinioFotosAdapterTest {

    private static final String URL_PUBLICA = "http://host-publico.test:9000";

    private static S3Client s3;
    private static S3Presigner presigner;
    private static S3Presigner presignerPublico;
    // * Firma con el host real del container, para poder descargar con la URL
    private static MinioFotosAdapter adaptador;
    // * Firma con un host "público" distinto del interno, como en docker-compose
    private static MinioFotosAdapter adaptadorConHostPublico;

    @BeforeAll
    static void levantarMinioSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con MinIO real");

        MinIOContainer minio = MinioTestContainer.iniciar();
        AlmacenamientoConfig config = new AlmacenamientoConfig();
        s3 = config.s3Client(minio.getS3URL(), MinioTestContainer.USUARIO, MinioTestContainer.CLAVE);
        presigner = config.s3Presigner(minio.getS3URL(), MinioTestContainer.USUARIO, MinioTestContainer.CLAVE);
        presignerPublico = config.s3Presigner(URL_PUBLICA, MinioTestContainer.USUARIO, MinioTestContainer.CLAVE);
        MinioTestContainer.crearBucketSiFalta(s3);
        adaptador = new MinioFotosAdapter(s3, presigner, MinioTestContainer.BUCKET, 60);
        adaptadorConHostPublico = new MinioFotosAdapter(s3, presignerPublico, MinioTestContainer.BUCKET, 60);
    }

    @AfterAll
    static void cerrarClientes() {
        if (s3 != null) {
            s3.close();
            presigner.close();
            presignerPublico.close();
        }
    }

    private static int estadoDeDescarga(String urlFirmada, byte[][] cuerpo) throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) URI.create(urlFirmada).toURL().openConnection();
        int estado = conexion.getResponseCode();
        if (estado == 200) {
            cuerpo[0] = conexion.getInputStream().readAllBytes();
        }
        return estado;
    }

    @Test
    void guardarYLuegoLaUrlPrefirmadaDescargaElMismoContenido() throws Exception {
        byte[] contenido = {1, 2, 3, 4, 5};
        adaptador.guardar("usuarios/u1/a.png", contenido, "image/png");

        byte[][] descargado = new byte[1][];
        int estado = estadoDeDescarga(adaptador.urlDeLectura("usuarios/u1/a.png"), descargado);

        assertThat(estado).isEqualTo(200);
        assertThat(descargado[0]).isEqualTo(contenido);
    }

    @Test
    void laUrlFirmadaUsaElHostPublicoYNoElInterno() {
        String url = adaptadorConHostPublico.urlDeLectura("usuarios/u2/b.png");

        assertThat(url).startsWith(URL_PUBLICA + "/" + MinioTestContainer.BUCKET + "/usuarios/u2/b.png?")
            .contains("X-Amz-Signature=")
            .doesNotContain(MinioTestContainer.iniciar().getS3URL());
    }

    @Test
    void borrarDejaDeServirElObjeto() throws Exception {
        adaptador.guardar("usuarios/u3/c.png", new byte[] {9}, "image/png");
        String url = adaptador.urlDeLectura("usuarios/u3/c.png");

        adaptador.borrar("usuarios/u3/c.png");

        assertThat(estadoDeDescarga(url, new byte[1][])).isEqualTo(404);
    }

    @Test
    void borrarDosVecesNoFalla() {
        adaptador.guardar("usuarios/u4/d.png", new byte[] {9}, "image/png");

        adaptador.borrar("usuarios/u4/d.png");

        assertThatCode(() -> adaptador.borrar("usuarios/u4/d.png")).doesNotThrowAnyException();
        assertThatCode(() -> adaptador.borrar("usuarios/nunca/existio.png")).doesNotThrowAnyException();
    }
}
