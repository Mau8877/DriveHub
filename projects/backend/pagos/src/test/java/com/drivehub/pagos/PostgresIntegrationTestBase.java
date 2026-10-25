package com.drivehub.pagos;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de los tests que necesitan un Postgres real (Flyway incluido).
 *
 * * Un solo container para toda la ejecución de tests: Spring cachea el contexto entre clases con la
 * * misma configuración, y ese contexto apunta a la URL del container; si cada clase levantara y
 * * detuviera el suyo, las clases siguientes reusarían un contexto apuntando a un container muerto.
 * * El container se maneja a mano (no con @Testcontainers/@Container) para poder saltear el test si
 * * Docker no está disponible en vez de romper el build (ver TESTING_BACKEND.md#testcontainers).
 */
public abstract class PostgresIntegrationTestBase {

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void levantarPostgresSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con base real");
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>("postgres:18-alpine");
            postgres.start();
        }
    }

    /**
     * Vacía todas las tablas de Pagos en el orden de sus claves foráneas. Los tests comparten la base: cada uno
     * parte de cero y deja todo limpio, sin importar qué más escribió el servicio (comprobantes, billeteras...).
     */
    protected static void vaciarTablas(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        String[] tablas = {"comprobante", "movimiento_horas", "billetera_horas", "reversion_pago",
            "transaccion_pasarela", "outbox_evento", "pago", "plan_pago"};
        for (String tabla : tablas) {
            jdbc.update("DELETE FROM " + tabla);
        }
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }
}
