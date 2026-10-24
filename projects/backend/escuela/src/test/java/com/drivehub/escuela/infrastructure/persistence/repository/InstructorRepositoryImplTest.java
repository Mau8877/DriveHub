package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.escuela.PostgresIntegrationTestBase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * CU09: las fichas de instructores contra un Postgres real (Flyway incluido).
 *
 * * Cada prueba usa su propia escuela (un UUID nuevo) y números de licencia únicos: la base se
 * * comparte entre pruebas.
 */
@SpringBootTest
class InstructorRepositoryImplTest extends PostgresIntegrationTestBase {

    @Autowired
    private InstructorRepository instructores;

    @Autowired
    private CategoriaLicenciaRepository categorias;

    @Autowired
    private JdbcTemplate jdbc;

    private static String licenciaUnica() {
        return "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Set<CategoriaLicencia> categorias(String... siglas) {
        return new LinkedHashSet<>(categorias.buscarPorSiglas(List.of(siglas)));
    }

    private Instructor ficha(UUID escuela, String licencia, String... siglas) {
        return Instructor.builder()
            .codigo(UuidV7.generar()).usuarioCodigo(UUID.randomUUID()).escuelaCodigo(escuela)
            .nroLicencia(licencia).licenciaVence(LocalDate.of(2030, 1, 1)).estado(EstadoInstructor.ACTIVO)
            .fechaContratacion(LocalDate.of(2026, 1, 1)).categorias(categorias(siglas)).build();
    }

    private Instructor guardar(UUID escuela, String... siglas) {
        return instructores.guardar(ficha(escuela, licenciaUnica(), siglas));
    }

    @Test
    void guardar_yBuscarPorCodigo_devuelvenLaFichaConSusCategoriasEnElOrdenDelCatalogo() {
        UUID escuela = UUID.randomUUID();
        Instructor original = ficha(escuela, licenciaUnica(), "C", "A");

        Instructor guardada = instructores.guardar(original);
        Instructor leida = instructores.buscarPorCodigo(escuela, original.getCodigo()).orElseThrow();

        assertThat(guardada.getCodigo()).isEqualTo(original.getCodigo());
        assertThat(leida.getUsuarioCodigo()).isEqualTo(original.getUsuarioCodigo());
        assertThat(leida.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(leida.getNroLicencia()).isEqualTo(original.getNroLicencia());
        assertThat(leida.getLicenciaVence()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(leida.getEstado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(leida.getCreadoEn()).isNotNull();
        // * El catálogo se sembró en este orden: A antes que C
        assertThat(leida.getCategorias()).extracting(CategoriaLicencia::getSigla).containsExactly("A", "C");
    }

    @Test
    void buscarPorCodigo_conOtraEscuela_devuelveVacio() {
        Instructor deA = guardar(UUID.randomUUID(), "B");

        assertThat(instructores.buscarPorCodigo(UUID.randomUUID(), deA.getCodigo())).isEmpty();
        assertThat(instructores.buscarPorCodigo(deA.getEscuelaCodigo(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void guardar_conUnNumeroDeLicenciaRepetido_lanzaErrEsc08() {
        UUID escuela = UUID.randomUUID();
        String licencia = licenciaUnica();
        instructores.guardar(ficha(escuela, licencia, "B"));

        assertThatThrownBy(() -> instructores.guardar(ficha(UUID.randomUUID(), licencia, "B")))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
    }

    @Test
    void listar_aislaPorEscuelaYOrdenaDelAltaMasRecienteALaMasAntigua() {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Instructor primero = guardar(escuelaA, "B");
        Instructor segundo = guardar(escuelaA, "B");
        Instructor tercero = guardar(escuelaA, "B");
        guardar(escuelaB, "B");

        Pagina<Instructor> primeraPagina = instructores.listar(escuelaA, null, null, 1, 2);
        Pagina<Instructor> segundaPagina = instructores.listar(escuelaA, null, null, 2, 2);

        assertThat(primeraPagina.elementos()).extracting(Instructor::getCodigo)
            .containsExactly(tercero.getCodigo(), segundo.getCodigo());
        assertThat(segundaPagina.elementos()).extracting(Instructor::getCodigo).containsExactly(primero.getCodigo());
        assertThat(primeraPagina.total()).isEqualTo(3);
    }

    @Test
    void listar_filtraPorEstadoYPorCodigosDePersona() {
        UUID escuela = UUID.randomUUID();
        Instructor activo = guardar(escuela, "B");
        Instructor deLicencia = guardar(escuela, "B");
        jdbc.update("UPDATE instructor SET estado = 'LICENCIA' WHERE codigo_instructor = ?", deLicencia.getCodigo());

        assertThat(instructores.listar(escuela, EstadoInstructor.LICENCIA, null, 1, 20).elementos())
            .extracting(Instructor::getCodigo).containsExactly(deLicencia.getCodigo());
        assertThat(instructores.listar(escuela, null, List.of(activo.getUsuarioCodigo()), 1, 20).elementos())
            .extracting(Instructor::getCodigo).containsExactly(activo.getCodigo());
        assertThat(instructores.listar(escuela, null, List.of(), 1, 20).elementos()).isEmpty();
        assertThat(instructores.listar(escuela, null, List.of(), 1, 20).total()).isZero();
        assertThat(instructores.listar(escuela, null, null, 1, 20).total()).isEqualTo(2);
    }

    @Test
    void existePorNroLicencia_yExistePorNroLicenciaDeOtro_ignoranALaPropiaFicha() {
        Instructor guardada = guardar(UUID.randomUUID(), "B");

        assertThat(instructores.existePorNroLicencia(guardada.getNroLicencia())).isTrue();
        assertThat(instructores.existePorNroLicencia("NO-EXISTE-XYZ")).isFalse();
        assertThat(instructores.existePorNroLicenciaDeOtro(guardada.getNroLicencia(), guardada.getCodigo())).isFalse();
        assertThat(instructores.existePorNroLicenciaDeOtro(guardada.getNroLicencia(), UUID.randomUUID())).isTrue();
    }

    @Test
    void actualizar_reemplazaLasCategoriasSinDuplicarlasYNoTocaUsuarioNiEscuela() {
        UUID escuela = UUID.randomUUID();
        Instructor original = guardar(escuela, "A", "B");

        Instructor editada = instructores.actualizar(original.toBuilder()
            .nroLicencia(licenciaUnica()).licenciaVence(LocalDate.of(2031, 2, 2)).estado(EstadoInstructor.INACTIVO)
            .fechaContratacion(LocalDate.of(2025, 5, 5)).categorias(categorias("B", "C")).build());
        Instructor leida = instructores.buscarPorCodigo(escuela, original.getCodigo()).orElseThrow();

        assertThat(editada.getCategorias()).extracting(CategoriaLicencia::getSigla).containsExactly("B", "C");
        assertThat(leida.getCategorias()).extracting(CategoriaLicencia::getSigla).containsExactly("B", "C");
        assertThat(leida.getEstado()).isEqualTo(EstadoInstructor.INACTIVO);
        assertThat(leida.getLicenciaVence()).isEqualTo(LocalDate.of(2031, 2, 2));
        assertThat(leida.getFechaContratacion()).isEqualTo(LocalDate.of(2025, 5, 5));
        assertThat(leida.getUsuarioCodigo()).isEqualTo(original.getUsuarioCodigo());
        assertThat(leida.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM instructor_categoria ic JOIN instructor i "
            + "ON i.id = ic.instructor_id WHERE i.codigo_instructor = ?", Integer.class, original.getCodigo()))
            .isEqualTo(2);
    }

    @Test
    void actualizar_conElNumeroDeOtroInstructor_lanzaErrEsc08() {
        UUID escuela = UUID.randomUUID();
        Instructor uno = guardar(escuela, "B");
        Instructor otro = guardar(escuela, "B");

        assertThatThrownBy(() -> instructores.actualizar(otro.toBuilder().nroLicencia(uno.getNroLicencia()).build()))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
    }

    @Test
    void buscarPorSiglas_noDistingueMayusculasYOmiteLasQueNoExisten() {
        assertThat(categorias.buscarPorSiglas(List.of("b", "A", "ZZZ")))
            .extracting(CategoriaLicencia::getSigla).containsExactly("A", "B");
        assertThat(categorias.buscarPorSiglas(List.of())).isEmpty();
    }

    @Test
    void laMigracion_dejaEscuelaCodigoNotNull() {
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns "
            + "WHERE table_name = 'instructor' AND column_name = 'escuela_codigo'", String.class)).isEqualTo("NO");
    }
}
