package com.drivehub.pagos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Arranca el contexto completo contra un Postgres real (Testcontainers, con Flyway): si una migración, un Model o
 * una consulta no coinciden, el servicio no arranca. Sin Docker se salta (ver PostgresIntegrationTestBase).
 */
@SpringBootTest
class PagosApplicationTests extends PostgresIntegrationTestBase {

    @Test
    void contextLoads() {
    }

}
