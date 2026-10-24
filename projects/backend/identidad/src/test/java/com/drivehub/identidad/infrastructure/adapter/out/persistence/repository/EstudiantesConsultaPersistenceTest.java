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
import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.FiltroEstudiantes;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU08: la consulta de estudiantes de una escuela contra un Postgres real (con `unaccent`).
 *
 * * Cada prueba usa su propia escuela (un UUID nuevo): la base se comparte entre pruebas.
 */
@SpringBootTest
class EstudiantesConsultaPersistenceTest extends PostgresIntegrationTestBase {

    private static final FiltroEstudiantes SIN_FILTRO = new FiltroEstudiantes(null, null);

    @Autowired
    private UsuarioRepositoryPort usuarios;

    @Autowired
    private RolRepositoryPort roles;

    private Usuario crear(UUID escuela, String rol, String nombres, String apellidos, EstadoUsuario estado) {
        Rol elRol = roles.buscarPorNombre(rol).orElseThrow();
        return usuarios.guardar(Usuario.builder()
            .email(UUID.randomUUID() + "@gmail.com").passwordHash("$2a$hash")
            .ci(CiDePrueba.unico()).sexo(SexoPersona.MASCULINO)
            .nombres(nombres).apellidos(apellidos).fechaNacimiento(LocalDate.of(2000, 1, 1))
            .escuelaCodigo(escuela).estado(estado).intentosFallidos(0).roles(Set.of(elRol)).build());
    }

    private Usuario estudiante(UUID escuela, String nombres, String apellidos) {
        return crear(escuela, RolNombres.ESTUDIANTE, nombres, apellidos, EstadoUsuario.ACTIVO);
    }

    private List<String> nombresDe(PaginaUsuarios pagina) {
        return pagina.elementos().stream().map(u -> u.getNombres() + " " + u.getApellidos()).toList();
    }

    @Test
    void listar_traeSoloEstudiantesDeLaEscuelaPedida() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        estudiante(escuelaA, "Ana", "Alfa");
        crear(escuelaA, RolNombres.INSTRUCTOR, "Ivan", "Instructor", EstadoUsuario.ACTIVO);
        crear(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin", EstadoUsuario.ACTIVO);
        estudiante(escuelaB, "Beto", "Beta");

        PaginaUsuarios pagina = usuarios.listarEstudiantesDeEscuela(escuelaA, SIN_FILTRO, 1, 20);

        assertThat(nombresDe(pagina)).containsExactly("Ana Alfa");
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    void listar_traeElPerfilEnLaMismaConsulta() {
        UUID escuela = UUID.randomUUID();
        estudiante(escuela, "Ana", "Alfa");

        Usuario resultado = usuarios.listarEstudiantesDeEscuela(escuela, SIN_FILTRO, 1, 20).elementos().get(0);

        assertThat(resultado.getPerfil().getCi()).isNotBlank();
        assertThat(resultado.getPerfil().getSexo()).isEqualTo(SexoPersona.MASCULINO);
        assertThat(resultado.getCreadoEn()).isNotNull();
    }

    @Test
    void listar_activoIncluyeLasBloqueadasEInactivoNo() {
        UUID escuela = UUID.randomUUID();
        crear(escuela, RolNombres.ESTUDIANTE, "Ana", "Activa", EstadoUsuario.ACTIVO);
        crear(escuela, RolNombres.ESTUDIANTE, "Berta", "Bloqueada", EstadoUsuario.BLOQUEADO);
        crear(escuela, RolNombres.ESTUDIANTE, "Ines", "Inactiva", EstadoUsuario.INACTIVO);

        PaginaUsuarios activos = usuarios.listarEstudiantesDeEscuela(
            escuela, new FiltroEstudiantes(null, EstadoFiltroEstudiante.ACTIVO), 1, 20);
        PaginaUsuarios inactivos = usuarios.listarEstudiantesDeEscuela(
            escuela, new FiltroEstudiantes(null, EstadoFiltroEstudiante.INACTIVO), 1, 20);
        PaginaUsuarios todos = usuarios.listarEstudiantesDeEscuela(escuela, SIN_FILTRO, 1, 20);

        assertThat(nombresDe(activos)).containsExactlyInAnyOrder("Ana Activa", "Berta Bloqueada");
        assertThat(nombresDe(inactivos)).containsExactly("Ines Inactiva");
        assertThat(todos.total()).isEqualTo(3);
    }

    @Test
    void listar_buscaSinDistinguirTildesNiMayusculas() {
        UUID escuela = UUID.randomUUID();
        estudiante(escuela, "José", "Gómez");
        estudiante(escuela, "Juan Carlos", "Pérez");
        estudiante(escuela, "María", "López");

        assertThat(nombresDe(buscar(escuela, "jose"))).containsExactly("José Gómez");
        assertThat(nombresDe(buscar(escuela, "JOSÉ"))).containsExactly("José Gómez");
        assertThat(nombresDe(buscar(escuela, "perez"))).containsExactly("Juan Carlos Pérez");
        assertThat(nombresDe(buscar(escuela, "maria lopez"))).containsExactly("María López");
    }

    @Test
    void listar_conVariasPalabrasExigeQueCadaUnaAparezca() {
        UUID escuela = UUID.randomUUID();
        estudiante(escuela, "Juan Carlos", "Pérez");
        estudiante(escuela, "Juan", "Rojas");

        assertThat(nombresDe(buscar(escuela, "juan perez"))).containsExactly("Juan Carlos Pérez");
        assertThat(nombresDe(buscar(escuela, "  carlos   juan "))).containsExactly("Juan Carlos Pérez");
        assertThat(buscar(escuela, "juan").total()).isEqualTo(2);
        assertThat(buscar(escuela, "juan zzz").total()).isZero();
    }

    @Test
    void listar_tomaLosComodinesDelTextoComoLetras() {
        UUID escuela = UUID.randomUUID();
        estudiante(escuela, "Ana", "Alfa");
        estudiante(escuela, "Beto", "Beta");

        assertThat(buscar(escuela, "%").total()).isZero();
        assertThat(buscar(escuela, "_").total()).isZero();
        assertThat(buscar(escuela, "\\").total()).isZero();
    }

    @Test
    void listar_ordenaPorAltaDescendenteYPaginaCuentaElTotal() {
        UUID escuela = UUID.randomUUID();
        estudiante(escuela, "Uno", "Primero");
        estudiante(escuela, "Dos", "Segundo");
        estudiante(escuela, "Tres", "Tercero");

        PaginaUsuarios primera = usuarios.listarEstudiantesDeEscuela(escuela, SIN_FILTRO, 1, 2);
        PaginaUsuarios segunda = usuarios.listarEstudiantesDeEscuela(escuela, SIN_FILTRO, 2, 2);

        assertThat(nombresDe(primera)).containsExactly("Tres Tercero", "Dos Segundo");
        assertThat(nombresDe(segunda)).containsExactly("Uno Primero");
        assertThat(primera.total()).isEqualTo(3);
        assertThat(segunda.total()).isEqualTo(3);
    }

    @Test
    void listar_enUnaEscuelaSinEstudiantes_devuelveVacio() {
        PaginaUsuarios pagina = usuarios.listarEstudiantesDeEscuela(UUID.randomUUID(), SIN_FILTRO, 1, 20);

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.total()).isZero();
    }

    @Test
    void buscarEstudianteDeEscuela_conEstudianteDeEsaEscuela_loDevuelveConSuPerfil() {
        UUID escuela = UUID.randomUUID();
        Usuario guardado = estudiante(escuela, "Ana", "Alfa");

        assertThat(usuarios.buscarEstudianteDeEscuela(escuela, guardado.getCodigo())).hasValueSatisfying(u -> {
            assertThat(u.getCodigo()).isEqualTo(guardado.getCodigo());
            assertThat(u.getPerfil().getNombres()).isEqualTo("Ana");
        });
    }

    @Test
    void buscarEstudianteDeEscuela_conCodigoDeOtraEscuelaODeUnInstructor_devuelveVacio() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario deB = estudiante(escuelaB, "Beto", "Beta");
        Usuario instructorDeA = crear(escuelaA, RolNombres.INSTRUCTOR, "Ivan", "Instructor", EstadoUsuario.ACTIVO);

        assertThat(usuarios.buscarEstudianteDeEscuela(escuelaA, deB.getCodigo())).isEmpty();
        assertThat(usuarios.buscarEstudianteDeEscuela(escuelaA, instructorDeA.getCodigo())).isEmpty();
        assertThat(usuarios.buscarEstudianteDeEscuela(escuelaA, UUID.randomUUID())).isEmpty();
    }

    private PaginaUsuarios buscar(UUID escuela, String texto) {
        return usuarios.listarEstudiantesDeEscuela(escuela, new FiltroEstudiantes(texto, null), 1, 20);
    }
}
