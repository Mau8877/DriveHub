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
    void tablaUsuario_tieneEscuelaCodigoYLaFechaDeNacimientoViveEnPerfil() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'usuario'", String.class);
        List<String> columnasDePerfil = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'perfil'", String.class);

        // * CU08a: los datos de la persona se mudaron a perfil
        assertThat(columnas).contains("escuela_codigo").doesNotContain("fecha_nacimiento");
        assertThat(columnasDePerfil).contains("fecha_nacimiento");
    }
}
