package com.drivehub.academico;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base de los tests que necesitan un MongoDB real (replica set de un nodo, igual que docker-compose).
 *
 * * Un solo container para toda la ejecución de tests: Spring cachea el contexto entre clases con la
 * * misma configuración, y ese contexto apunta a la URL del container; si cada clase levantara y
 * * detuviera el suyo, las clases siguientes reusarían un contexto apuntando a un container muerto.
 * * El container se maneja a mano (no con @Testcontainers/@Container) para poder saltear el test si
 * * Docker no está disponible en vez de romper el build (ver TESTING_BACKEND.md#testcontainers).
 */
public abstract class MongoIntegrationTestBase {

    private static MongoDBContainer mongo;

    @BeforeAll
    static void levantarMongoSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con base real");
        if (mongo == null) {
            mongo = new MongoDBContainer("mongo:8");
            mongo.start();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> mongo.getReplicaSetUrl("academico"));
    }
}
