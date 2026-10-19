package com.drivehub.escuela.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.infrastructure.persistence.repository.EscuelaJpaRepository;

/**
 * El seeder corre solo con el perfil `dev` y es idempotente.
 */
@SpringBootTest
@ActiveProfiles("dev")
class EscuelaSeederTest {

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private EscuelaSeeder seeder;

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
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test del seeder");
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
    void run_ejecutadoDosVeces_dejaUnaSolaEscuelaDeEjemplo() {
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        long cantidad = escuelaJpaRepository.findAll().stream()
            .filter(e -> EscuelaSeeder.SLUG_ESCUELA_EXAMPLE.equals(e.getSlug()))
            .count();
        assertThat(cantidad).isEqualTo(1);
        assertThat(escuelaJpaRepository.findBySlugAndEstado(
                EscuelaSeeder.SLUG_ESCUELA_EXAMPLE, EstadoEscuela.ACTIVA))
            .hasValueSatisfying(e ->
                assertThat(e.getCodigo()).isEqualTo(EscuelaSeeder.CODIGO_ESCUELA_EXAMPLE));
    }
}
