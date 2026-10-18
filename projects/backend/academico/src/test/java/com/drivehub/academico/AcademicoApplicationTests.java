package com.drivehub.academico;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Arranca el contexto completo contra la base real del servicio (en CI la
 * levanta el job como service container, ver .github/workflows/ci.yml).
 */
@SpringBootTest
class AcademicoApplicationTests {

    @Test
    void contextLoads() {
    }

}
