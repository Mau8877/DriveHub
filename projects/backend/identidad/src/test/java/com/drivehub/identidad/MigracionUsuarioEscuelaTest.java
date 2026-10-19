package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La migración de CU01: columnas nuevas de `usuario` y los 4 roles.
 */
@SpringBootTest
class MigracionUsuarioEscuelaTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void tablaRol_tieneLosCuatroRolesDelSistemaYNoElViejoAdmin() {
        List<String> roles = jdbc.queryForList("SELECT nombre FROM rol", String.class);

        assertThat(roles).containsExactlyInAnyOrder("SUPER_ADMIN", "ADMIN_ESCUELA", "INSTRUCTOR", "ESTUDIANTE");
    }

    @Test
    void tablaUsuario_tieneFechaDeNacimientoYEscuelaCodigo() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'usuario'", String.class);

        assertThat(columnas).contains("fecha_nacimiento", "escuela_codigo");
    }
}
