package com.drivehub.pagos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * El servicio arranca sin base de datos, sin MinIO y sin credenciales: con la configuración por defecto.
 *
 * * Sirve de red de seguridad que corre en cualquier máquina (también sin Docker): Hibernate valida TODAS las
 * * consultas JPQL de los repositorios al crear el contexto, y un bean que exija una credencial vacía rompe aquí.
 * * Flyway se apaga y no se abre ninguna conexión; las migraciones las prueban los tests con Testcontainers.
 */
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "spring.datasource.hikari.initialization-fail-timeout=-1"
})
class ArranqueSinBaseTest {

    @Test
    void elContextoArranca_yTodasLasConsultasJpqlSonValidas() {
    }
}
