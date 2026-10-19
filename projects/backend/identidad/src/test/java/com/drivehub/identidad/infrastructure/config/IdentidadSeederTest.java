package com.drivehub.identidad.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.drivehub.identidad.PostgresIntegrationTestBase;

/**
 * El seeder corre solo con el perfil `dev` y es idempotente.
 */
@SpringBootTest
@ActiveProfiles("dev")
class IdentidadSeederTest extends PostgresIntegrationTestBase {

    @Autowired
    private IdentidadSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void run_ejecutadoDosVeces_dejaExactamenteLosDosUsuariosDeEjemplo() {
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        Integer total = jdbc.queryForObject(
            "SELECT count(*) FROM usuario WHERE email IN (?, ?)", Integer.class,
            IdentidadSeeder.EMAIL_SUPER_ADMIN, IdentidadSeeder.EMAIL_ADMIN_EXAMPLE);
        assertThat(total).isEqualTo(2);
    }

    @Test
    void run_creaElSuperAdminSinEscuelaYElAdminConLaEscuelaDeEjemplo() {
        seeder.run(new DefaultApplicationArguments());

        String rolSuper = jdbc.queryForObject(
            "SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = ? AND u.escuela_codigo IS NULL",
            String.class, IdentidadSeeder.EMAIL_SUPER_ADMIN);
        String rolAdmin = jdbc.queryForObject(
            "SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = ? AND u.escuela_codigo = ?::uuid",
            String.class, IdentidadSeeder.EMAIL_ADMIN_EXAMPLE, IdentidadSeeder.CODIGO_ESCUELA_EXAMPLE.toString());
        String hash = jdbc.queryForObject(
            "SELECT password_hash FROM usuario WHERE email = ?", String.class, IdentidadSeeder.EMAIL_SUPER_ADMIN);

        assertThat(rolSuper).isEqualTo("SUPER_ADMIN");
        assertThat(rolAdmin).isEqualTo("ADMIN_ESCUELA");
        assertThat(hash).startsWith("$2").isNotEqualTo("123123123");
    }
}
