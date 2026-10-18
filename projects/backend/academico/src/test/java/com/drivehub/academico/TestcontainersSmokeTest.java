package com.drivehub.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

/**
 * Prueba mínima de que Testcontainers puede levantar un MongoDB real (replica set) desde
 * DENTRO del contenedor del servicio (necesita el socket de Docker montado,
 * ver docker-compose.yml). No usa el contexto de Spring a propósito -- es
 * solo para confirmar que el mecanismo funciona antes de escribir tests de
 * integración reales sobre repositorios.
 *
 * * El ciclo de vida del container se maneja a mano (try-with-resources en
 * * el test, no @Container/@Testcontainers) para poder chequear si Docker
 * * está disponible ANTES de intentar levantar nada, y saltear el test en
 * * vez de romper el build si no lo está.
 * ! Docker Desktop en Windows a veces expone un socket "stub" restringido
 * ! en vez del daemon real al montarlo en un contenedor (falla con "Could
 * ! not find a valid Docker environment" aunque /var/run/docker.sock esté
 * ! montado) -- hay que revisar Docker Desktop > Settings > Advanced >
 * ! "Allow the default Docker socket to be used" en la máquina host.
 */
class TestcontainersSmokeTest {

    @BeforeAll
    static void saltarSiNoHayDockerDisponible() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible,
                "Docker no está disponible desde este contenedor -- se salta en vez de "
                        + "romper el build. Ver TESTING_BACKEND.md#testcontainers.");
    }

    @Test
    void elContenedorDeMongoLevantaYAceptaConexiones() {
        // MongoDBContainer arranca un replica set de un nodo, igual que docker-compose.
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:8")) {
            mongo.start();
            assertThat(mongo.isRunning()).isTrue();

            try (MongoClient cliente = MongoClients.create(mongo.getReplicaSetUrl("academico"))) {
                Document ping = cliente.getDatabase("academico").runCommand(new Document("ping", 1));
                assertThat(ping.get("ok", Number.class).intValue()).isEqualTo(1);
            }
        }
    }
}
