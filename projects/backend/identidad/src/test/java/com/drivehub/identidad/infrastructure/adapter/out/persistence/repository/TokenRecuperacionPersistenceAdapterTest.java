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
import org.springframework.dao.DataIntegrityViolationException;

import com.drivehub.identidad.PostgresIntegrationTestBase;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * Enlaces de correo (verificación y recuperación) contra un Postgres real.
 */
@SpringBootTest
class TokenRecuperacionPersistenceAdapterTest extends PostgresIntegrationTestBase {

    @Autowired
    private TokenRecuperacionRepositoryPort tokens;

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

    private TokenRecuperacion token(Usuario u, String hash, TipoTokenCorreo tipo) {
        return tokens.guardar(TokenRecuperacion.builder().usuarioCodigo(u.getCodigo()).tipo(tipo).tokenHash(hash)
            .expiraEn(Instant.now().plusSeconds(3600)).build());
    }

    @Test
    void guardarYBuscarPorHash_devuelveElEnlaceConSuUsuarioYTipo() {
        Usuario u = usuario("token1@gmail.com");
        TokenRecuperacion guardado = token(u, "a".repeat(64), TipoTokenCorreo.VERIFICACION);

        TokenRecuperacion leido = tokens.buscarPorHash("a".repeat(64)).orElseThrow();

        assertThat(leido.getCodigo()).isEqualTo(guardado.getCodigo());
        assertThat(leido.getUsuarioCodigo()).isEqualTo(u.getCodigo());
        assertThat(leido.getTipo()).isEqualTo(TipoTokenCorreo.VERIFICACION);
        assertThat(leido.isUsado()).isFalse();
        assertThat(tokens.buscarPorHash("z".repeat(64))).isEmpty();
    }

    @Test
    void invalidarPendientes_soloTocaElUsuarioYElTipoIndicados() {
        Usuario u = usuario("token2@gmail.com");
        Usuario otro = usuario("token3@gmail.com");
        token(u, "b".repeat(64), TipoTokenCorreo.VERIFICACION);
        token(u, "c".repeat(64), TipoTokenCorreo.RECUPERACION);
        token(otro, "d".repeat(64), TipoTokenCorreo.VERIFICACION);

        tokens.invalidarPendientes(u.getCodigo(), TipoTokenCorreo.VERIFICACION);

        assertThat(tokens.buscarPorHash("b".repeat(64)).orElseThrow().isUsado()).isTrue();
        assertThat(tokens.buscarPorHash("c".repeat(64)).orElseThrow().isUsado()).isFalse();
        assertThat(tokens.buscarPorHash("d".repeat(64)).orElseThrow().isUsado()).isFalse();
    }

    @Test
    void marcarUsado_dejaElEnlaceConsumido() {
        Usuario u = usuario("token4@gmail.com");
        TokenRecuperacion guardado = token(u, "e".repeat(64), TipoTokenCorreo.RECUPERACION);

        tokens.marcarUsado(guardado);

        assertThat(tokens.buscarPorHash("e".repeat(64)).orElseThrow().isUsado()).isTrue();
    }

    @Test
    void guardar_conUnHashRepetido_fallaPorSerUnico() {
        Usuario u = usuario("token5@gmail.com");
        token(u, "f".repeat(64), TipoTokenCorreo.VERIFICACION);

        assertThatThrownBy(() -> token(u, "f".repeat(64), TipoTokenCorreo.VERIFICACION))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
