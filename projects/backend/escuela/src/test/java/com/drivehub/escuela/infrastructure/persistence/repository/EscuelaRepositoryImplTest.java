package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.domain.repository.FiltroEscuelas;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

/**
 * Repositorio de escuelas contra un Postgres real (Flyway incluido).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no
 * * está disponible (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
class EscuelaRepositoryImplTest {

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private EscuelaRepository escuelaRepository;

    @Autowired
    private EscuelaJpaRepository escuelaJpaRepository;

    @BeforeAll
    static void levantarPostgresSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test de repositorio");
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
    void buscarActivaPorSlug_conEscuelaActiva_laEncuentra() {
        escuelaJpaRepository.save(new EscuelaModel(
            UUID.randomUUID(), "activa-repo", "Escuela Activa"));

        assertThat(escuelaRepository.buscarActivaPorSlug("activa-repo"))
            .hasValueSatisfying(e -> assertThat(e.getNombre()).isEqualTo("Escuela Activa"));
    }

    private EscuelaModel guardarEliminada(UUID codigo, String slug, String nombre) {
        EscuelaModel model = new EscuelaModel(codigo, slug, nombre);
        model.setDeletedAt(Instant.now());
        return escuelaJpaRepository.saveAndFlush(model);
    }

    @Test
    void buscarActivaPorSlug_conEscuelaEliminadaLogicamente_noLaEncuentra() {
        guardarEliminada(UUID.randomUUID(), "inactiva-repo", "Escuela Inactiva");

        assertThat(escuelaRepository.buscarActivaPorSlug("inactiva-repo")).isEmpty();
    }

    @Test
    void buscarPorCodigo_encuentraUnaEscuelaEliminadaLogicamenteYNoUnaInexistente() {
        UUID codigo = UUID.randomUUID();
        guardarEliminada(codigo, "suspendida-repo", "Suspendida");

        assertThat(escuelaRepository.buscarPorCodigo(codigo)).hasValueSatisfying(e -> {
            assertThat(e.activa()).isFalse();
            assertThat(e.estado()).isEqualTo(EstadoEscuela.INACTIVA);
            assertThat(e.getEliminadaEn()).isNotNull();
        });
        assertThat(escuelaRepository.buscarPorCodigo(UUID.randomUUID())).isEmpty();
    }

    @Test
    void buscarPorCodigo_conEscuelaActiva_laDevuelveActiva() {
        UUID codigo = UUID.randomUUID();
        escuelaJpaRepository.save(new EscuelaModel(codigo, "viva-repo", "Viva"));

        assertThat(escuelaRepository.buscarPorCodigo(codigo))
            .hasValueSatisfying(e -> assertThat(e.estado()).isEqualTo(EstadoEscuela.ACTIVA));
    }

    @Test
    void existePorSlugIncluyendoEliminadas_ve_tambienLasEliminadas() {
        guardarEliminada(UUID.randomUUID(), "reservado-repo", "Reservado");

        assertThat(escuelaJpaRepository.existePorSlugIncluyendoEliminadas("reservado-repo")).isTrue();
        assertThat(escuelaJpaRepository.existePorSlugIncluyendoEliminadas("nadie-repo")).isFalse();
    }

    @Test
    void buscarActivaPorSlug_conSlugInexistente_devuelveVacio() {
        assertThat(escuelaRepository.buscarActivaPorSlug("no-existe-repo")).isEmpty();
    }

    private Escuela nueva(String slug, String nombre) {
        return Escuela.builder().codigo(UUID.randomUUID()).slug(slug).nombre(nombre).build();
    }

    @Test
    void guardar_creaLaEscuelaActivaConElCodigoDado() {
        Escuela entrada = nueva("guardada-repo", "Guardada");

        Escuela guardada = escuelaRepository.guardar(entrada);

        assertThat(guardada.getCodigo()).isEqualTo(entrada.getCodigo());
        assertThat(guardada.activa()).isTrue();
        assertThat(guardada.getCreadoEn()).isNotNull();
        assertThat(escuelaRepository.buscarActivaPorSlug("guardada-repo")).isPresent();
    }

    @Test
    void guardar_conSlugRepetido_fallaEnElMismoInstante() {
        escuelaRepository.guardar(nueva("duplicada-repo", "Una"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> escuelaRepository.guardar(nueva("duplicada-repo", "Otra")))
            .isInstanceOfSatisfying(com.drivehub.escuela.common.exception.ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(com.drivehub.escuela.common.exception.EscuelaErrorCodes.SLUG_EN_USO));
    }

    @Test
    void existePorSlug_incluyeLasEscuelasDesactivadas() {
        guardarEliminada(UUID.randomUUID(), "reservada-repo", "Reservada");

        assertThat(escuelaRepository.existePorSlug("reservada-repo")).isTrue();
        assertThat(escuelaRepository.existePorSlug("libre-repo")).isFalse();
    }

    @Test
    void listar_ordenaDeLaMasRecienteALaMasAntiguaYMuestraActivasEInactivas() {
        escuelaRepository.guardar(nueva("orden-uno", "Orden Uno"));
        escuelaRepository.guardar(nueva("orden-dos", "Orden Dos"));
        guardarEliminada(UUID.randomUUID(), "orden-tres", "Orden Tres");

        Pagina<Escuela> pagina = escuelaRepository.listar(new FiltroEscuelas("orden-", null), 1, 10);

        assertThat(pagina.total()).isEqualTo(3);
        assertThat(pagina.elementos()).extracting(Escuela::getSlug)
            .containsExactly("orden-tres", "orden-dos", "orden-uno");
        assertThat(pagina.elementos().get(0).estado()).isEqualTo(EstadoEscuela.INACTIVA);
    }

    @Test
    void listar_buscaPorNombreOSlugSinDistinguirMayusculasYTrataLosComodinesComoTexto() {
        escuelaRepository.guardar(nueva("busca-autos", "Escuela Norte"));
        escuelaRepository.guardar(nueva("busca-sur", "Escuela AUTOS del Sur"));
        escuelaRepository.guardar(nueva("busca-otra", "Otra"));
        escuelaRepository.guardar(nueva("busca-pct", "Cien%"));

        assertThat(escuelaRepository.listar(new FiltroEscuelas("AUTOS", null), 1, 10).elementos())
            .extracting(Escuela::getSlug).containsExactlyInAnyOrder("busca-autos", "busca-sur");
        assertThat(escuelaRepository.listar(new FiltroEscuelas("  ", null), 1, 100).total())
            .isGreaterThanOrEqualTo(4);
        assertThat(escuelaRepository.listar(new FiltroEscuelas("%", null), 1, 10).elementos())
            .extracting(Escuela::getSlug).containsExactly("busca-pct");
    }

    @Test
    void listar_filtraPorEstado() {
        escuelaRepository.guardar(nueva("estado-viva", "Estado Viva"));
        guardarEliminada(UUID.randomUUID(), "estado-muerta", "Estado Muerta");

        assertThat(escuelaRepository.listar(new FiltroEscuelas("estado-", EstadoEscuela.ACTIVA), 1, 10).elementos())
            .extracting(Escuela::getSlug).containsExactly("estado-viva");
        assertThat(escuelaRepository.listar(new FiltroEscuelas("estado-", EstadoEscuela.INACTIVA), 1, 10).elementos())
            .extracting(Escuela::getSlug).containsExactly("estado-muerta");
    }

    @Test
    void listar_paginaConPaginaUnoIndexada() {
        for (int i = 1; i <= 5; i++) {
            escuelaRepository.guardar(nueva("pagina-" + i, "Pagina " + i));
        }

        Pagina<Escuela> primera = escuelaRepository.listar(new FiltroEscuelas("pagina-", null), 1, 2);
        Pagina<Escuela> tercera = escuelaRepository.listar(new FiltroEscuelas("pagina-", null), 3, 2);

        assertThat(primera.total()).isEqualTo(5);
        assertThat(primera.elementos()).extracting(Escuela::getSlug).containsExactly("pagina-5", "pagina-4");
        assertThat(tercera.elementos()).extracting(Escuela::getSlug).containsExactly("pagina-1");
    }

    @Test
    void actualizarNombre_cambiaSoloElNombreDeUnaEscuelaActiva() {
        Escuela guardada = escuelaRepository.guardar(nueva("nombre-repo", "Nombre Viejo"));

        Escuela cambiada = escuelaRepository.actualizarNombre(guardada.getCodigo(), "Nombre Nuevo").orElseThrow();

        assertThat(cambiada.getNombre()).isEqualTo("Nombre Nuevo");
        assertThat(cambiada.getSlug()).isEqualTo("nombre-repo");
    }

    @Test
    void actualizarNombre_conEscuelaInexistenteODesactivada_devuelveVacio() {
        UUID eliminada = UUID.randomUUID();
        guardarEliminada(eliminada, "nombre-eliminada", "Eliminada");

        assertThat(escuelaRepository.actualizarNombre(UUID.randomUUID(), "X")).isEmpty();
        assertThat(escuelaRepository.actualizarNombre(eliminada, "X")).isEmpty();
    }

    @Test
    void desactivarYReactivar_sonIdempotentesYDevuelvenSiCambioAlgo() {
        Escuela guardada = escuelaRepository.guardar(nueva("alterna-repo", "Alterna"));
        UUID codigo = guardada.getCodigo();

        assertThat(escuelaRepository.desactivar(codigo)).isTrue();
        assertThat(escuelaRepository.desactivar(codigo)).isFalse();
        assertThat(escuelaRepository.buscarActivaPorSlug("alterna-repo")).isEmpty();
        assertThat(escuelaRepository.buscarPorCodigo(codigo)).hasValueSatisfying(e -> assertThat(e.activa()).isFalse());

        assertThat(escuelaRepository.reactivar(codigo)).isTrue();
        assertThat(escuelaRepository.reactivar(codigo)).isFalse();
        assertThat(escuelaRepository.buscarActivaPorSlug("alterna-repo")).isPresent();
        assertThat(escuelaRepository.desactivar(UUID.randomUUID())).isFalse();
    }
}
