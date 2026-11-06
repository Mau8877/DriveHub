package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.identidad.CiDePrueba;
import com.drivehub.identidad.PostgresIntegrationTestBase;
import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

/**
 * Vínculos y códigos de WhatsApp (CU47) contra un Postgres real.
 */
@SpringBootTest
class WhatsappPersistenceAdaptersTest extends PostgresIntegrationTestBase {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Autowired
    private VinculoWhatsappRepositoryPort vinculos;

    @Autowired
    private DesafioWhatsappRepositoryPort desafios;

    @Autowired
    private UsuarioRepositoryPort usuarios;

    @Autowired
    private RolRepositoryPort roles;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario usuario(String email) {
        Rol rol = roles.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow();
        return usuarios.guardar(Usuario.builder().email(email).passwordHash("$2a$hash").ci(CiDePrueba.unico())
            .sexo(SexoPersona.MASCULINO).nombres("Ana").apellidos("Pérez").fechaNacimiento(LocalDate.of(2000, 1, 1))
            .escuelaCodigo(UUID.randomUUID()).estado(EstadoUsuario.ACTIVO).roles(Set.of(rol)).build());
    }

    private DesafioWhatsapp desafio(Usuario u, String telefonoHash, String correoHash, Instant expira) {
        return desafios.guardar(DesafioWhatsapp.builder().usuarioCodigo(u == null ? null : u.getCodigo())
            .telefonoHash(telefonoHash).correoHash(correoHash).codigoHash("c".repeat(64)).expiraEn(expira).build());
    }

    @Test
    void reemplazar_dejaUnSoloVinculoPorNumeroYPorCuenta() {
        Usuario ana = usuario("wa1@gmail.com");
        Usuario luis = usuario("wa2@gmail.com");

        vinculos.reemplazar(ana.getCodigo(), HASH_A);
        vinculos.reemplazar(luis.getCodigo(), HASH_A);

        assertThat(vinculos.buscarPorTelefonoHash(HASH_A).orElseThrow().getUsuarioCodigo()).isEqualTo(luis.getCodigo());
        assertThat(vinculos.buscarPorUsuario(ana.getCodigo())).isEmpty();

        vinculos.reemplazar(luis.getCodigo(), HASH_B);

        assertThat(vinculos.buscarPorTelefonoHash(HASH_A)).isEmpty();
        assertThat(vinculos.buscarPorTelefonoHash(HASH_B).orElseThrow().getUsuarioCodigo()).isEqualTo(luis.getCodigo());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vinculo_whatsapp", Integer.class)).isEqualTo(1);
    }

    @Test
    void buscarVigente_devuelveElMasRecienteSinUsarNiVencer() {
        Usuario ana = usuario("wa3@gmail.com");
        DesafioWhatsapp viejo = desafio(ana, HASH_A, "x".repeat(64), Instant.now().plusSeconds(600));
        desafios.invalidarPendientes(ana.getCodigo());
        DesafioWhatsapp nuevo = desafio(ana, HASH_A, "x".repeat(64), Instant.now().plusSeconds(600));

        DesafioWhatsapp vigente = desafios.buscarVigente(ana.getCodigo(), HASH_A, Instant.now()).orElseThrow();

        assertThat(vigente.getCodigo()).isEqualTo(nuevo.getCodigo()).isNotEqualTo(viejo.getCodigo());
        assertThat(desafios.buscarVigente(ana.getCodigo(), HASH_B, Instant.now())).isEmpty();
        assertThat(desafios.buscarVigente(ana.getCodigo(), HASH_A, Instant.now().plusSeconds(3600))).isEmpty();
    }

    @Test
    void intentosYUso_seGuardanSobreElCodigo() {
        Usuario ana = usuario("wa4@gmail.com");
        DesafioWhatsapp d = desafio(ana, HASH_A, "y".repeat(64), Instant.now().plusSeconds(600));

        desafios.registrarIntentoFallido(d.getCodigo());
        desafios.registrarIntentoFallido(d.getCodigo());

        assertThat(desafios.buscarVigente(ana.getCodigo(), HASH_A, Instant.now()).orElseThrow().getIntentos())
            .isEqualTo(2);

        desafios.marcarUsado(d.getCodigo());

        assertThat(desafios.buscarVigente(ana.getCodigo(), HASH_A, Instant.now())).isEmpty();
    }

    @Test
    void contar_incluyeLosPedidosDescartadosSinCuenta() {
        String tel = "d".repeat(64);
        String correo = "e".repeat(64);
        desafio(null, tel, correo, Instant.now());
        desafio(null, tel, correo, Instant.now());
        Instant haceUnMinuto = Instant.now().minusSeconds(60);

        assertThat(desafios.contarPorTelefonoDesde(tel, haceUnMinuto)).isEqualTo(2);
        assertThat(desafios.contarPorCorreoDesde(correo, haceUnMinuto)).isEqualTo(2);
        assertThat(desafios.contarPorTelefonoDesde(tel, Instant.now().plusSeconds(60))).isZero();
        assertThat(desafios.contarPorTelefonoDesde(HASH_B, haceUnMinuto)).isZero();
    }
}
