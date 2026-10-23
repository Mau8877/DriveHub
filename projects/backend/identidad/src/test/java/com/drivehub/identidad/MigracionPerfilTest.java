package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * La migración de CU08a (`V20261023100000__crear_perfil.sql`): los datos de la persona pasan de
 * `usuario` a `perfil` sin perder a nadie.
 *
 * * Levanta su propio Postgres porque necesita migrar en dos tiempos (hasta la versión anterior,
 * * con cuentas ya cargadas, y luego hasta la última). Sin Docker, el test se saltea.
 */
class MigracionPerfilTest {

    private static final String VERSION_ANTERIOR = "20261021120000";

    @Test
    void migracion_copiaLosDatosDeCadaCuentaAlPerfilYQuitaLasColumnasDeUsuario() {
        assumeTrue(dockerDisponible(), "Docker no está disponible: se salta el test con base real");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")) {
            postgres.start();
            migrar(postgres, VERSION_ANTERIOR);
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));

            insertarCuenta(jdbc, "super@gmail.com", null, "SUPER_ADMIN", null, null);
            insertarCuenta(jdbc, "admin@gmail.com", "0198f0a0-0000-7000-8000-000000000001", "ADMIN_ESCUELA",
                "0991111111", "usuarios/a/foto.png");
            insertarCuenta(jdbc, "estudiante@gmail.com", "0198f0a0-0000-7000-8000-000000000001", "ESTUDIANTE",
                null, null);

            migrar(postgres, null);

            assertThat(jdbc.queryForObject("SELECT count(*) FROM perfil", Integer.class))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class))
                .isEqualTo(3);

            Map<String, Object> admin = fila(jdbc, "admin@gmail.com");
            assertThat(admin.get("nombres")).isEqualTo("Nombre");
            assertThat(admin.get("apellidos")).isEqualTo("Apellido");
            assertThat(admin.get("telefono")).isEqualTo("0991111111");
            assertThat(admin.get("foto_url")).isEqualTo("usuarios/a/foto.png");
            assertThat(admin.get("fecha_nacimiento").toString()).isEqualTo("1990-01-01");
            assertThat(admin.get("sexo")).isEqualTo("MASCULINO");
            assertThat(admin.get("direccion")).isEqualTo("X");
            assertThat(admin.get("categoria_sigla")).isNull();

            assertThat(fila(jdbc, "super@gmail.com").get("direccion")).isNull();
            assertThat(fila(jdbc, "estudiante@gmail.com").get("categoria_sigla")).isEqualTo("SIN");

            List<String> cis = jdbc.queryForList("SELECT ci FROM perfil", String.class);
            assertThat(cis).doesNotHaveDuplicates().allSatisfy(ci -> assertThat(ci).startsWith("X"));

            List<String> columnasDeUsuario = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'usuario'", String.class);
            assertThat(columnasDeUsuario)
                .doesNotContain("nombres", "apellidos", "telefono", "foto_url", "fecha_nacimiento")
                .contains("email", "password_hash", "escuela_codigo", "estado");
        }
    }

    private static Map<String, Object> fila(JdbcTemplate jdbc, String email) {
        return jdbc.queryForMap("SELECT p.* FROM perfil p JOIN usuario u ON u.id = p.usuario_id WHERE u.email = ?",
            email);
    }

    private static void insertarCuenta(JdbcTemplate jdbc, String email, String escuela, String rol, String telefono,
            String foto) {
        jdbc.update("INSERT INTO usuario (codigo_usuario, email, password_hash, nombres, apellidos, telefono, foto_url,"
                + " fecha_nacimiento, escuela_codigo, estado)"
                + " VALUES (gen_random_uuid(), ?, '$2a$hash', 'Nombre', 'Apellido', ?, ?, DATE '1990-01-01',"
                + " ?::uuid, 'ACTIVO')",
            email, telefono, foto, escuela);
        jdbc.update("INSERT INTO usuario_rol (codigo_usuario_rol, usuario_id, rol_id)"
                + " SELECT gen_random_uuid(), u.id, r.id FROM usuario u, rol r WHERE u.email = ? AND r.nombre = ?",
            email, rol);
    }

    private static void migrar(PostgreSQLContainer<?> postgres, String hastaVersion) {
        var configuracion = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration");
        if (hastaVersion != null) {
            configuracion.target(hastaVersion);
        }
        configuracion.load().migrate();
    }

    private static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
