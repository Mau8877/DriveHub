package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.escuela.PostgresIntegrationTestBase;
import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * CU29: las dos consultas de alumnos de un instructor contra un Postgres real (Flyway incluido).
 *
 * * Cada prueba usa su propia escuela y sus propios instructores (UUID nuevos): la base se comparte.
 */
@SpringBootTest
class AsignacionAlumnoRepositoryImplTest extends PostgresIntegrationTestBase {

    @Autowired
    private AsignacionAlumnoRepository asignaciones;

    @Autowired
    private JdbcTemplate jdbc;

    private long instructor(UUID escuela, UUID usuario) {
        return jdbc.queryForObject("""
            INSERT INTO instructor (codigo_instructor, usuario_codigo, escuela_codigo, nro_licencia)
            VALUES (?, ?, ?, ?) RETURNING id
            """, Long.class, UUID.randomUUID(), usuario, escuela, "LIC-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private void asignar(long instructorId, UUID estudiante, String fechaFin) {
        jdbc.update("""
            INSERT INTO asignacion_alumno (codigo_asignacion_alumno, instructor_id, estudiante_codigo, fecha_inicio,
                fecha_fin) VALUES (?, ?, ?, '2026-01-01', ?::date)
            """, UUID.randomUUID(), instructorId, estudiante, fechaFin);
    }

    @Test
    void buscar_traeSoloLosAlumnosVigentesDeEseInstructorYDeEsaEscuela() {
        UUID escuela = UUID.randomUUID();
        UUID instructorA = UUID.randomUUID();
        UUID instructorB = UUID.randomUUID();
        UUID vigente1 = UUID.randomUUID();
        UUID vigente2 = UUID.randomUUID();
        long a = instructor(escuela, instructorA);
        long b = instructor(escuela, instructorB);
        asignar(a, vigente1, null);
        asignar(a, vigente2, null);
        asignar(a, UUID.randomUUID(), "2026-02-01");
        asignar(b, UUID.randomUUID(), null);

        Pagina<UUID> deA = asignaciones.buscarEstudiantesDeInstructor(escuela, instructorA, 1, 20);

        assertThat(deA.elementos()).containsExactly(vigente1, vigente2);
        assertThat(deA.total()).isEqualTo(2);
        assertThat(asignaciones.buscarEstudiantesDeInstructor(UUID.randomUUID(), instructorA, 1, 20).total()).isZero();
    }

    @Test
    void buscar_paginaYCuentaElTotal() {
        UUID escuela = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        long id = instructor(escuela, usuario);
        for (int i = 0; i < 5; i++) {
            asignar(id, UUID.randomUUID(), null);
        }

        Pagina<UUID> segunda = asignaciones.buscarEstudiantesDeInstructor(escuela, usuario, 2, 2);

        assertThat(segunda.elementos()).hasSize(2);
        assertThat(segunda.total()).isEqualTo(5);
    }

    @Test
    void existeAsignacionActiva_soloParaElAlumnoVigenteDeEseInstructor() {
        UUID escuela = UUID.randomUUID();
        UUID usuarioA = UUID.randomUUID();
        UUID usuarioB = UUID.randomUUID();
        UUID alumno = UUID.randomUUID();
        UUID exAlumno = UUID.randomUUID();
        long a = instructor(escuela, usuarioA);
        instructor(escuela, usuarioB);
        asignar(a, alumno, null);
        asignar(a, exAlumno, "2026-02-01");

        assertThat(asignaciones.existeAsignacionActiva(escuela, usuarioA, alumno)).isTrue();
        assertThat(asignaciones.existeAsignacionActiva(escuela, usuarioB, alumno)).isFalse();
        assertThat(asignaciones.existeAsignacionActiva(UUID.randomUUID(), usuarioA, alumno)).isFalse();
        assertThat(asignaciones.existeAsignacionActiva(escuela, usuarioA, exAlumno)).isFalse();
        assertThat(asignaciones.existeAsignacionActiva(escuela, usuarioA, UUID.randomUUID())).isFalse();
    }
}
