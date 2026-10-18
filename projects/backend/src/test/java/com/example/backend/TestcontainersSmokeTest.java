package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Prueba mínima de que Testcontainers puede levantar un Postgres real desde
 * DENTRO del contenedor del backend (necesita el socket de Docker montado,
 * ver docker-compose.yml). No usa el contexto de Spring a propósito -- es
 * solo para confirmar que el mecanismo funciona antes de escribir tests de
 * integración reales sobre repositorios/Flyway.
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
    void elContenedorDePostgresLevantaYAceptaConexiones() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")) {
            postgres.start();
            assertThat(postgres.isRunning()).isTrue();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                assertThat(connection.isValid(2)).isTrue();
            }
        }
    }
}
