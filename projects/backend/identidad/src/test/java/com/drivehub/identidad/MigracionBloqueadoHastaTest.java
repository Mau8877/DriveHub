package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La migración de CU02: columna para el bloqueo temporal de cuentas.
 */
@SpringBootTest
class MigracionBloqueadoHastaTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void tablaUsuario_tieneLaColumnaBloqueadoHasta() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'usuario'", String.class);

        assertThat(columnas).contains("bloqueado_hasta");
    }
}
