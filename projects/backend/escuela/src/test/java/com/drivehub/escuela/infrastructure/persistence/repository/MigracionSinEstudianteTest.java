package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * La migración de CU08a (`V20261023100000__escuela_sin_estudiante_y_categorias.sql`): Escuela deja
 * de tener tabla de estudiantes y el catálogo de categorías de licencia queda sembrado.
 *
 * * Contra un Postgres real (Flyway incluido). El container se maneja a mano para poder saltear
 * * el test si Docker no está disponible (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
class MigracionSinEstudianteTest {

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void levantarPostgresSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test de migración");
        postgres = new PostgreSQLContainer<>("postgres:18-alpine");
        postgres.start();
    }

    @AfterAll
    static void detenerPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @Test
    void categoriaLicencia_tieneLasSeisCategoriasSembradasConSinCategoria() {
        List<String> siglas = jdbc.queryForList("SELECT sigla FROM categoria_licencia ORDER BY id", String.class);

        assertThat(siglas).containsExactly("P", "M", "A", "B", "C", "SIN");
        assertThat(jdbc.queryForObject("SELECT descripcion FROM categoria_licencia WHERE sigla = 'SIN'", String.class))
            .isEqualTo("Sin categoría");
    }

    @Test
    void categoriaLicencia_laSiglaEsUnica() {
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO categoria_licencia (codigo_categoria_licencia, sigla, descripcion) VALUES (?, 'SIN', 'Otra')",
            UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void tablaEstudiante_yaNoExiste() {
        Integer tablas = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'estudiante'", Integer.class);

        assertThat(tablas).isZero();
    }

    @Test
    void instructor_yaNoRepiteElCi() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'instructor'", String.class);

        assertThat(columnas).doesNotContain("ci").contains("usuario_codigo", "nro_licencia");
    }

    @Test
    void asignacionAlumno_apuntaALaPersonaPorSuCodigoYSoloAdmiteUnaAsignacionVigente() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'asignacion_alumno'", String.class);
        assertThat(columnas).contains("estudiante_codigo").doesNotContain("estudiante_id");

        UUID instructor = UUID.randomUUID();
        UUID estudiante = UUID.randomUUID();
        jdbc.update("INSERT INTO instructor (codigo_instructor, usuario_codigo, nro_licencia) VALUES (?, ?, 'LIC-1')",
            instructor, UUID.randomUUID());
        Long instructorId = jdbc.queryForObject("SELECT id FROM instructor WHERE codigo_instructor = ?", Long.class,
            instructor);
        String insertar = "INSERT INTO asignacion_alumno (codigo_asignacion_alumno, instructor_id, estudiante_codigo,"
            + " fecha_inicio) VALUES (?, ?, ?, CURRENT_DATE)";
        jdbc.update(insertar, UUID.randomUUID(), instructorId, estudiante);

        assertThatThrownBy(() -> jdbc.update(insertar, UUID.randomUUID(), instructorId, estudiante))
            .isInstanceOf(Exception.class);
    }
}
