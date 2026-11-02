package com.drivehub.academico.features.clases.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;
import com.drivehub.academico.features.clases.model.PruebaClase;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class PruebaClaseRepositoryTest extends MongoIntegrationTestBase {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Autowired
    private PruebaClaseRepository repository;

    @Autowired
    private ResultadoRepository resultadoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final UUID instructor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices
        mongoTemplate.remove(new Query(), PruebaClase.class);
        mongoTemplate.remove(new Query(), Resultado.class);
    }

    private PruebaClase guardar(Long claseId, TipoPrueba tipo) {
        return repository.save(new PruebaClase(claseId, estudiante, instructor, tipo,
            List.of(new CriterioEvaluacion("espejos", "Posición y uso de espejos", 8, 10)), 80.0,
            ResultadoEvaluacion.APROBADO, null, HOY));
    }

    @Test
    void buscarPorClaseYTipo_encuentraLaGuardadaYNoLaBorrada() {
        PruebaClase prueba = guardar(1L, TipoPrueba.MANEJO);

        assertThat(repository.findByClaseIdAndTipoAndDeletedAtIsNull(1L, TipoPrueba.MANEJO)).isPresent()
            .get().extracting(PruebaClase::getCodigoPruebaClase).isEqualTo(prueba.getCodigoPruebaClase());
        assertThat(repository.findByClaseIdAndTipoAndDeletedAtIsNull(1L, TipoPrueba.CIUDAD)).isEmpty();

        prueba.setDeletedAt(Instant.now());
        repository.save(prueba);
        assertThat(repository.findByClaseIdAndTipoAndDeletedAtIsNull(1L, TipoPrueba.MANEJO)).isEmpty();
    }

    @Test
    void listarPorClase_devuelveLaPaginaYElTotal() {
        guardar(1L, TipoPrueba.MANEJO);
        guardar(1L, TipoPrueba.CIUDAD);
        guardar(2L, TipoPrueba.MANEJO);

        var pagina = repository.findByClaseIdAndDeletedAtIsNull(1L, PageRequest.of(0, 1));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
    }

    @Test
    void buscarVariasClases_devuelveLasPruebasDeTodas() {
        guardar(1L, TipoPrueba.MANEJO);
        guardar(2L, TipoPrueba.CIUDAD);
        guardar(3L, TipoPrueba.CIUDAD);

        assertThat(repository.findByClaseIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .extracting(PruebaClase::getClaseId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void dosPruebasDelMismoTipoEnLaMismaClase_lanzanDuplicateKey() {
        guardar(1L, TipoPrueba.MANEJO);

        assertThatThrownBy(() -> guardar(1L, TipoPrueba.MANEJO)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void otroTipoOtraClase_sePuedenGuardar() {
        guardar(1L, TipoPrueba.MANEJO);

        assertThat(guardar(1L, TipoPrueba.CIUDAD)).isNotNull();
        assertThat(guardar(2L, TipoPrueba.MANEJO)).isNotNull();
    }

    @Test
    void existeLaNota_soloParaElOrigenYElIdDeLaPrueba() {
        Resultado nota = new Resultado();
        nota.setEscuelaCodigo(UUID.randomUUID());
        nota.setEstudianteCodigo(estudiante);
        nota.setOrigen(OrigenResultado.PRUEBA_CLASE);
        nota.setOrigenId(300L);
        nota.setTitulo("Prueba de clase: Manejo");
        nota.setPuntaje(80);
        nota.setAprobado(true);
        nota.setFecha(Instant.now());
        resultadoRepository.save(nota);

        assertThat(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE,
            300L)).isTrue();
        assertThat(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(
            OrigenResultado.EXAMEN_CONDUCCION, 300L)).isFalse();
        assertThat(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE,
            301L)).isFalse();
    }

    @Test
    void dosNotasDeLaMismaEvaluacion_lanzanDuplicateKey_peroOtroOrigenConElMismoIdSePuede() {
        resultadoRepository.save(nota(OrigenResultado.PRUEBA_CLASE, 400L));

        assertThatThrownBy(() -> resultadoRepository.save(nota(OrigenResultado.PRUEBA_CLASE, 400L)))
            .isInstanceOf(DuplicateKeyException.class);
        assertThat(resultadoRepository.save(nota(OrigenResultado.EXAMEN_CONDUCCION, 400L))).isNotNull();
        assertThat(resultadoRepository.save(nota(OrigenResultado.PRUEBA_CLASE, 401L))).isNotNull();
    }

    private Resultado nota(OrigenResultado origen, Long origenId) {
        Resultado nota = new Resultado();
        nota.setEscuelaCodigo(UUID.randomUUID());
        nota.setEstudianteCodigo(estudiante);
        nota.setOrigen(origen);
        nota.setOrigenId(origenId);
        nota.setTitulo("Nota");
        nota.setPuntaje(80);
        nota.setAprobado(true);
        nota.setFecha(Instant.now());
        return nota;
    }
}
