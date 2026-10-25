package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.drivehub.identidad.CiDePrueba;
import com.drivehub.identidad.PostgresIntegrationTestBase;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU09: la consulta de instructores de una escuela contra un Postgres real (con `unaccent`).
 *
 * * Cada prueba usa su propia escuela (un UUID nuevo): la base se comparte entre pruebas.
 */
@SpringBootTest
class InstructoresConsultaPersistenceTest extends PostgresIntegrationTestBase {

    @Autowired
    private UsuarioRepositoryPort usuarios;

    @Autowired
    private RolRepositoryPort roles;

    private Usuario crear(UUID escuela, String rol, String nombres, String apellidos) {
        Rol elRol = roles.buscarPorNombre(rol).orElseThrow();
        return usuarios.guardar(Usuario.builder()
            .email(UUID.randomUUID() + "@gmail.com").passwordHash("$2a$hash")
            .ci(CiDePrueba.unico()).sexo(SexoPersona.MASCULINO)
            .nombres(nombres).apellidos(apellidos).fechaNacimiento(LocalDate.of(1990, 1, 1))
            .escuelaCodigo(escuela).estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(Set.of(elRol)).build());
    }

    private Usuario instructor(UUID escuela, String nombres, String apellidos) {
        return crear(escuela, RolNombres.INSTRUCTOR, nombres, apellidos);
    }

    private List<String> nombresDe(PaginaUsuarios pagina) {
        return pagina.elementos().stream().map(u -> u.getNombres() + " " + u.getApellidos()).toList();
    }

    @Test
    void listar_traeSoloInstructoresDeLaEscuelaPedida() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        instructor(escuelaA, "Ivan", "Alfa");
        crear(escuelaA, RolNombres.ESTUDIANTE, "Eva", "Estudiante");
        crear(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        instructor(escuelaB, "Ines", "Beta");

        PaginaUsuarios pagina = usuarios.listarInstructoresDeEscuela(escuelaA, null, 1, 20);

        assertThat(nombresDe(pagina)).containsExactly("Ivan Alfa");
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    void listar_conBusquedaSinTildesYVariasPalabras_filtraPorNombreCompleto() {
        UUID escuela = UUID.randomUUID();
        instructor(escuela, "José Luis", "Pérez");
        instructor(escuela, "Marta", "Gómez");

        assertThat(nombresDe(usuarios.listarInstructoresDeEscuela(escuela, "jose perez", 1, 20)))
            .containsExactly("José Luis Pérez");
        assertThat(nombresDe(usuarios.listarInstructoresDeEscuela(escuela, "GOMEZ", 1, 20)))
            .containsExactly("Marta Gómez");
        assertThat(usuarios.listarInstructoresDeEscuela(escuela, "   ", 1, 20).total()).isEqualTo(2);
    }

    @Test
    void listar_conPorcentajeOGuionBajo_nolosTomaComoComodines() {
        UUID escuela = UUID.randomUUID();
        instructor(escuela, "Ana", "Alfa");

        assertThat(usuarios.listarInstructoresDeEscuela(escuela, "%", 1, 20).total()).isZero();
        assertThat(usuarios.listarInstructoresDeEscuela(escuela, "A_a", 1, 20).total()).isZero();
    }

    @Test
    void listar_ordenaDelAltaMasRecienteAlaMasAntiguaYElTotalEsElDeToda_laConsulta() {
        UUID escuela = UUID.randomUUID();
        instructor(escuela, "Uno", "Primero");
        instructor(escuela, "Dos", "Segundo");
        instructor(escuela, "Tres", "Tercero");

        PaginaUsuarios primera = usuarios.listarInstructoresDeEscuela(escuela, null, 1, 2);
        PaginaUsuarios segunda = usuarios.listarInstructoresDeEscuela(escuela, null, 2, 2);

        assertThat(nombresDe(primera)).containsExactly("Tres Tercero", "Dos Segundo");
        assertThat(nombresDe(segunda)).containsExactly("Uno Primero");
        assertThat(primera.total()).isEqualTo(3);
    }

    @Test
    void buscarPorCodigos_devuelveSoloInstructoresDeEsaEscuela() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario deA = instructor(escuelaA, "Ivan", "Alfa");
        Usuario deB = instructor(escuelaB, "Ines", "Beta");
        Usuario estudiante = crear(escuelaA, RolNombres.ESTUDIANTE, "Eva", "Estudiante");

        List<Usuario> encontrados = usuarios.buscarInstructoresPorCodigos(escuelaA,
            List.of(deA.getCodigo(), deB.getCodigo(), estudiante.getCodigo(), UUID.randomUUID()));

        assertThat(encontrados).extracting(Usuario::getCodigo).containsExactly(deA.getCodigo());
        assertThat(usuarios.buscarInstructoresPorCodigos(escuelaA, List.of())).isEmpty();
    }

    @Test
    void buscarEstudiantesPorCodigos_devuelveSoloEstudiantesDeEsaEscuela() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario deA = crear(escuelaA, RolNombres.ESTUDIANTE, "Eva", "Alfa");
        Usuario deB = crear(escuelaB, RolNombres.ESTUDIANTE, "Eli", "Beta");
        Usuario instructor = instructor(escuelaA, "Ivan", "Alfa");

        List<Usuario> encontrados = usuarios.buscarEstudiantesPorCodigos(escuelaA,
            List.of(deA.getCodigo(), deB.getCodigo(), instructor.getCodigo(), UUID.randomUUID()));

        assertThat(encontrados).extracting(Usuario::getCodigo).containsExactly(deA.getCodigo());
        assertThat(usuarios.buscarEstudiantesPorCodigos(escuelaA, List.of())).isEmpty();
    }

    @Test
    void buscarInstructorDeEscuela_conOtraEscuelaOUnEstudiante_devuelveVacio() {
        UUID escuelaA = UUID.randomUUID();
        Usuario deA = instructor(escuelaA, "Ivan", "Alfa");
        Usuario estudiante = crear(escuelaA, RolNombres.ESTUDIANTE, "Eva", "Estudiante");

        assertThat(usuarios.buscarInstructorDeEscuela(escuelaA, deA.getCodigo())).isPresent();
        assertThat(usuarios.buscarInstructorDeEscuela(UUID.randomUUID(), deA.getCodigo())).isEmpty();
        assertThat(usuarios.buscarInstructorDeEscuela(escuelaA, estudiante.getCodigo())).isEmpty();
        assertThat(usuarios.buscarInstructorDeEscuela(escuelaA, UUID.randomUUID())).isEmpty();
    }
}
