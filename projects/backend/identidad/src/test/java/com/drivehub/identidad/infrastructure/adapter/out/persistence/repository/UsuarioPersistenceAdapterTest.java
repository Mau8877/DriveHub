package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
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
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
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
    private RefreshTokenRepositoryPort refreshTokens;

    @Autowired
    private TokenRecuperacionRepositoryPort tokensDeCorreo;

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

    @Test
    void buscarPorEmail_devuelveLaCuentaConSusRolesYSuEscuela() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        Usuario guardado = usuarioRepository.guardar(nuevoEstudiante("lectura@gmail.com", estudiante));

        Usuario leido = usuarioRepository.buscarPorEmail("lectura@gmail.com").orElseThrow();

        assertThat(leido.getCodigo()).isEqualTo(guardado.getCodigo());
        assertThat(leido.getEscuelaCodigo()).isEqualTo(guardado.getEscuelaCodigo());
        assertThat(leido.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.ESTUDIANTE);
        assertThat(usuarioRepository.buscarPorCodigo(guardado.getCodigo())).isPresent();
        assertThat(usuarioRepository.buscarPorEmail("no-existe@gmail.com")).isEmpty();
    }

    @Test
    void actualizar_persisteElBloqueoLosIntentosYElUltimoIngreso() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        Usuario guardado = usuarioRepository.guardar(nuevoEstudiante("bloqueo@gmail.com", estudiante));
        java.time.Instant hasta = java.time.Instant.now().plusSeconds(900).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        usuarioRepository.actualizar(guardado.toBuilder()
            .estado(EstadoUsuario.BLOQUEADO).intentosFallidos(5).bloqueadoHasta(hasta).ultimoLogin(hasta).build());

        Usuario leido = usuarioRepository.buscarPorCodigo(guardado.getCodigo()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
        assertThat(leido.getIntentosFallidos()).isEqualTo(5);
        assertThat(leido.getBloqueadoHasta()).isEqualTo(hasta);
        assertThat(leido.getUltimoLogin()).isEqualTo(hasta);
    }

    @Test
    void correoVerificado_empiezaEnFalseYSePersisteAlActualizar() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        Usuario guardado = usuarioRepository.guardar(nuevoEstudiante("verificado@gmail.com", estudiante));
        assertThat(guardado.isCorreoVerificado()).isFalse();

        usuarioRepository.actualizar(guardado.toBuilder().correoVerificado(true).build());

        assertThat(usuarioRepository.buscarPorCodigo(guardado.getCodigo()).orElseThrow().isCorreoVerificado()).isTrue();
    }

    @Test
    void actualizar_persisteLaContrasenaNueva() {
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        Usuario guardado = usuarioRepository.guardar(nuevoEstudiante("clave@gmail.com", estudiante));

        usuarioRepository.actualizar(guardado.toBuilder().passwordHash("$2a$hash-nuevo").build());

        assertThat(usuarioRepository.buscarPorCodigo(guardado.getCodigo()).orElseThrow().getPasswordHash())
            .isEqualTo("$2a$hash-nuevo");
    }

    private Usuario nuevoAdministrador(String email, UUID escuela) {
        Rol admin = rolRepository.buscarPorNombre(RolNombres.ADMIN_ESCUELA).orElseThrow();
        return usuarioRepository.guardar(Usuario.builder()
            .email(email).passwordHash("$2a$hash").nombres("Marta").apellidos("Gómez")
            .fechaNacimiento(LocalDate.of(1985, 5, 5)).escuelaCodigo(escuela)
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(Set.of(admin)).build());
    }

    @Test
    void existeAdministradorDeEscuela_soloSiLaEscuelaTieneUnaCuentaAdminEscuela() {
        UUID conAdmin = UUID.randomUUID();
        UUID soloEstudiante = UUID.randomUUID();
        nuevoAdministrador("admin-existe@gmail.com", conAdmin);
        Rol estudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        usuarioRepository.guardar(nuevoEstudiante("est-existe@gmail.com", estudiante).toBuilder()
            .escuelaCodigo(soloEstudiante).build());

        assertThat(usuarioRepository.existeAdministradorDeEscuela(conAdmin)).isTrue();
        assertThat(usuarioRepository.existeAdministradorDeEscuela(soloEstudiante)).isFalse();
        assertThat(usuarioRepository.existeAdministradorDeEscuela(UUID.randomUUID())).isFalse();
    }

    @Test
    void buscarAdministradorDeEscuela_devuelveLaCuentaConSuRol() {
        UUID escuela = UUID.randomUUID();
        Usuario guardado = nuevoAdministrador("admin-busca@gmail.com", escuela);

        Usuario encontrado = usuarioRepository.buscarAdministradorDeEscuela(escuela).orElseThrow();

        assertThat(encontrado.getCodigo()).isEqualTo(guardado.getCodigo());
        assertThat(encontrado.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.ADMIN_ESCUELA);
        assertThat(usuarioRepository.buscarAdministradorDeEscuela(UUID.randomUUID())).isEmpty();
    }

    @Test
    void eliminarDefinitivamente_borraLaCuentaConTodoLoQueCuelgaDeEllaYLiberaElCorreo() {
        UUID escuela = UUID.randomUUID();
        Usuario admin = nuevoAdministrador("admin-borra@gmail.com", escuela);
        refreshTokens.guardar(RefreshToken.builder().usuarioCodigo(admin.getCodigo()).tokenHash("r".repeat(64))
            .dispositivo(Dispositivo.WEB).expiraEn(Instant.now().plusSeconds(3600)).build());
        tokensDeCorreo.guardar(TokenRecuperacion.builder().usuarioCodigo(admin.getCodigo())
            .tipo(TipoTokenCorreo.VERIFICACION).tokenHash("t".repeat(64)).expiraEn(Instant.now().plusSeconds(3600)).build());

        usuarioRepository.eliminarDefinitivamente(admin.getCodigo());

        assertThat(usuarioRepository.buscarPorCodigo(admin.getCodigo())).isEmpty();
        assertThat(usuarioRepository.existePorEmail("admin-borra@gmail.com")).isFalse();
        for (String tabla : new String[] {"usuario_rol", "refresh_token", "token_recuperacion"}) {
            Integer filas = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE usuario_id NOT IN"
                + " (SELECT id FROM usuario)", Integer.class);
            assertThat(filas).as("filas huérfanas en " + tabla).isZero();
        }
        // * El correo quedó libre: se puede volver a crear la cuenta
        assertThat(nuevoAdministrador("admin-borra@gmail.com", escuela)).isNotNull();
    }
}
