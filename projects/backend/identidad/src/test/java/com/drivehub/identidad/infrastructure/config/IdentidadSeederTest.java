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
    void run_ejecutadoDosVeces_dejaExactamenteLosTresUsuariosDeEjemplo() {
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        Integer total = jdbc.queryForObject(
            "SELECT count(*) FROM usuario WHERE email IN (?, ?, ?)", Integer.class,
            IdentidadSeeder.EMAIL_SUPER_ADMIN, IdentidadSeeder.EMAIL_ADMIN_EXAMPLE,
            IdentidadSeeder.EMAIL_INSTRUCTOR_EXAMPLE);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void run_creaElInstructorConLaEscuelaDeEjemplo() {
        seeder.run(new DefaultApplicationArguments());

        String rol = jdbc.queryForObject(
            "SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = ? AND u.escuela_codigo = ?::uuid",
            String.class, IdentidadSeeder.EMAIL_INSTRUCTOR_EXAMPLE, IdentidadSeeder.CODIGO_ESCUELA_EXAMPLE.toString());

        assertThat(rol).isEqualTo("INSTRUCTOR");
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

    @Test
    void run_creaElPerfilDeCadaCuentaConSuCiYLaUbicacionSoloDelAdminYDelInstructor() {
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email IN (?, ?, ?)", Integer.class, IdentidadSeeder.EMAIL_SUPER_ADMIN,
            IdentidadSeeder.EMAIL_ADMIN_EXAMPLE, IdentidadSeeder.EMAIL_INSTRUCTOR_EXAMPLE)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT p.ci FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = ?", String.class, IdentidadSeeder.EMAIL_ADMIN_EXAMPLE))
            .isEqualTo(IdentidadSeeder.CI_ADMIN_EXAMPLE);
        assertThat(jdbc.queryForObject("SELECT p.direccion FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = ?", String.class, IdentidadSeeder.EMAIL_ADMIN_EXAMPLE)).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT p.direccion FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = ?", String.class, IdentidadSeeder.EMAIL_SUPER_ADMIN)).isNull();
        assertThat(jdbc.queryForObject("SELECT p.latitud FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = ?", java.math.BigDecimal.class, IdentidadSeeder.EMAIL_SUPER_ADMIN)).isNull();
    }

    @Test
    void ciDeEjemplo_sonDistintosEntreSi() {
        assertThat(java.util.Set.of(IdentidadSeeder.CI_SUPER_ADMIN, IdentidadSeeder.CI_ADMIN_EXAMPLE,
            IdentidadSeeder.CI_INSTRUCTOR_EXAMPLE)).hasSize(3);
    }
}
