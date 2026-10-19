package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.identidad.PostgresIntegrationTestBase;
import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * Persistencia de usuarios y roles contra un Postgres real.
 */
@SpringBootTest
class UsuarioPersistenceAdapterTest extends PostgresIntegrationTestBase {

    @Autowired
    private UsuarioRepositoryPort usuarioRepository;

    @Autowired
    private RolRepositoryPort rolRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario nuevoEstudiante(String email, Rol rol) {
        return Usuario.builder()
            .email(email).passwordHash("$2a$hash").nombres("Ana").apellidos("Pérez")
            .fechaNacimiento(LocalDate.of(2000, 1, 1)).escuelaCodigo(UUID.randomUUID())
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(Set.of(rol)).build();
    }

    @Test
    void guardar_creaElUsuarioYSuAsociacionConElRol() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();

        Usuario guardado = usuarioRepository.guardar(nuevoEstudiante("guardar@gmail.com", estudiante));

        assertThat(guardado.getCodigo()).isNotNull();
        assertThat(usuarioRepository.existePorEmail("guardar@gmail.com")).isTrue();
        Integer asociaciones = jdbc.queryForObject(
            "SELECT count(*) FROM usuario_rol ur JOIN usuario u ON u.id = ur.usuario_id WHERE u.email = ?",
            Integer.class, "guardar@gmail.com");
        assertThat(asociaciones).isEqualTo(1);
    }

    @Test
    void guardar_conCorreoRepetido_lanzaConflictErrIde01() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        usuarioRepository.guardar(nuevoEstudiante("repetido@gmail.com", estudiante));

        assertThatThrownBy(() -> usuarioRepository.guardar(nuevoEstudiante("repetido@gmail.com", estudiante)))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
    }

    @Test
    void buscarPorNombre_encuentraLosCuatroRolesDelSistema() {
        assertThat(rolRepository.buscarPorNombre(RolNombres.SUPER_ADMIN)).isPresent();
        assertThat(rolRepository.buscarPorNombre(RolNombres.ADMIN_ESCUELA)).isPresent();
        assertThat(rolRepository.buscarPorNombre(RolNombres.INSTRUCTOR)).isPresent();
        assertThat(rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE)).isPresent();
        assertThat(rolRepository.buscarPorNombre("ADMIN")).isEmpty();
    }
}
