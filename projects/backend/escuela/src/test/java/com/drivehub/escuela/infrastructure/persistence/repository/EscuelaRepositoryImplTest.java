package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

/**
 * Repositorio de escuelas contra un Postgres real (Flyway incluido).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no
 * * está disponible (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
class EscuelaRepositoryImplTest {

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private EscuelaRepository escuelaRepository;

    @Autowired
    private EscuelaJpaRepository escuelaJpaRepository;

    @BeforeAll
    static void levantarPostgresSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test de repositorio");
        postgres = new PostgreSQLContainer<>("postgres:18-alpine");
        postgres.start();
    }

    @AfterAll
    static void detenerPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @Test
    void buscarActivaPorSlug_conEscuelaActiva_laEncuentra() {
        escuelaJpaRepository.save(new EscuelaModel(
            UUID.randomUUID(), "activa-repo", "Escuela Activa", EstadoEscuela.ACTIVA));

        assertThat(escuelaRepository.buscarActivaPorSlug("activa-repo"))
            .hasValueSatisfying(e -> assertThat(e.getNombre()).isEqualTo("Escuela Activa"));
    }

    @Test
    void buscarActivaPorSlug_conEscuelaInactiva_noLaEncuentra() {
        escuelaJpaRepository.save(new EscuelaModel(
            UUID.randomUUID(), "inactiva-repo", "Escuela Inactiva", EstadoEscuela.INACTIVA));

        assertThat(escuelaRepository.buscarActivaPorSlug("inactiva-repo")).isEmpty();
    }

    @Test
    void buscarActivaPorSlug_conSlugInexistente_devuelveVacio() {
        assertThat(escuelaRepository.buscarActivaPorSlug("no-existe-repo")).isEmpty();
    }
}
