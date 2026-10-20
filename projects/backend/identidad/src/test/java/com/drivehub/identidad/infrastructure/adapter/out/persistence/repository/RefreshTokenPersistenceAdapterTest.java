package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.drivehub.identidad.PostgresIntegrationTestBase;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * Sesiones renovables contra un Postgres real.
 */
@SpringBootTest
class RefreshTokenPersistenceAdapterTest extends PostgresIntegrationTestBase {

    @Autowired
    private RefreshTokenRepositoryPort refreshTokens;

    @Autowired
    private UsuarioRepositoryPort usuarios;

    @Autowired
    private RolRepositoryPort roles;

    private Usuario usuario(String email) {
        Rol rol = roles.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        return usuarios.guardar(Usuario.builder().email(email).passwordHash("$2a$hash").nombres("Ana").apellidos("Pérez")
            .fechaNacimiento(LocalDate.of(2000, 1, 1)).escuelaCodigo(UUID.randomUUID()).estado(EstadoUsuario.ACTIVO)
            .roles(Set.of(rol)).build());
    }

    private RefreshToken sesion(Usuario u, String hash, Dispositivo dispositivo) {
        return refreshTokens.guardar(RefreshToken.builder().usuarioCodigo(u.getCodigo()).tokenHash(hash)
            .dispositivo(dispositivo).expiraEn(Instant.now().plusSeconds(3600)).revocado(false).build());
    }

    @Test
    void guardarYBuscarPorHash_devuelveLaSesionConSuUsuario() {
        Usuario u = usuario("sesion1@gmail.com");
        RefreshToken guardado = sesion(u, "a".repeat(64), Dispositivo.WEB);

        RefreshToken leido = refreshTokens.buscarPorHash("a".repeat(64)).orElseThrow();

        assertThat(leido.getCodigo()).isEqualTo(guardado.getCodigo());
        assertThat(leido.getUsuarioCodigo()).isEqualTo(u.getCodigo());
        assertThat(leido.getDispositivo()).isEqualTo(Dispositivo.WEB);
        assertThat(leido.isRevocado()).isFalse();
        assertThat(refreshTokens.buscarPorHash("z".repeat(64))).isEmpty();
    }

    @Test
    void revocarActivos_revocaSoloLosDelDispositivoPedido() {
        Usuario u = usuario("sesion2@gmail.com");
        sesion(u, "b".repeat(64), Dispositivo.WEB);
        sesion(u, "c".repeat(64), Dispositivo.MOVIL);

        refreshTokens.revocarActivos(u.getCodigo(), Dispositivo.WEB);

        assertThat(refreshTokens.buscarPorHash("b".repeat(64)).orElseThrow().isRevocado()).isTrue();
        assertThat(refreshTokens.buscarPorHash("c".repeat(64)).orElseThrow().isRevocado()).isFalse();
    }

    @Test
    void revocarTodos_revocaLasSesionesDeLosDosDispositivosPeroNoLasDeOtroUsuario() {
        Usuario u = usuario("sesion4@gmail.com");
        Usuario otro = usuario("sesion5@gmail.com");
        sesion(u, "e".repeat(64), Dispositivo.WEB);
        sesion(u, "f".repeat(64), Dispositivo.MOVIL);
        sesion(otro, "g".repeat(64), Dispositivo.WEB);

        refreshTokens.revocarTodos(u.getCodigo());

        assertThat(refreshTokens.buscarPorHash("e".repeat(64)).orElseThrow().isRevocado()).isTrue();
        assertThat(refreshTokens.buscarPorHash("f".repeat(64)).orElseThrow().isRevocado()).isTrue();
        assertThat(refreshTokens.buscarPorHash("g".repeat(64)).orElseThrow().isRevocado()).isFalse();
    }

    @Test
    void revocar_marcaUnaSesionComoRevocada() {
        Usuario u = usuario("sesion3@gmail.com");
        RefreshToken guardado = sesion(u, "d".repeat(64), Dispositivo.MOVIL);

        refreshTokens.revocar(guardado);

        assertThat(refreshTokens.buscarPorHash("d".repeat(64)).orElseThrow().isRevocado()).isTrue();
    }
}
