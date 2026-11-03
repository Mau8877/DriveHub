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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.model.Clase;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class ClaseRepositoryTest extends MongoIntegrationTestBase {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Autowired
    private ClaseRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final UUID instructor = UUID.randomUUID();
    private final UUID otroInstructor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices
        mongoTemplate.remove(new Query(), Clase.class);
    }

    private Clase guardar(Long reservaId, UUID elInstructor) {
        return repository.save(new Clase(reservaId, estudiante, elInstructor, HOY, Asistencia.PRESENTE));
    }

    @Test
    void claseNueva_nacerCerradaConSuCodigoYSinVehiculoNiHorasReales() {
        Clase clase = guardar(1L, instructor);

        assertThat(clase.getCodigoClase()).isNotNull();
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.CERRADA);
        assertThat(clase.getVehiculoCodigo()).isNull();
        assertThat(clase.getInicioReal()).isNull();
        assertThat(clase.getFinReal()).isNull();
    }

    @Test
    void buscarPorCodigoEInstructor_encuentraLaPropia() {
        Clase clase = guardar(1L, instructor);

        var encontrada = repository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(clase.getCodigoClase(),
            instructor);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getReservaId()).isEqualTo(1L);
    }

    @Test
    void buscarPorCodigoEInstructor_sinCoincidenciaDevuelveVacio() {
        Clase ajena = guardar(1L, otroInstructor);
        Clase borrada = guardar(2L, instructor);
        borrada.setDeletedAt(Instant.parse("2026-10-26T13:00:00Z"));
        repository.save(borrada);

        assertThat(repository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(ajena.getCodigoClase(),
            instructor)).isEmpty();
        assertThat(repository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(borrada.getCodigoClase(),
            instructor)).isEmpty();
        assertThat(repository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(UUID.randomUUID(), instructor))
            .isEmpty();
    }

    @Test
    void buscarPorReserva_encuentraLaDeEsaReservaYNoLaBorrada() {
        guardar(5L, instructor);
        Clase borrada = guardar(6L, instructor);
        borrada.setDeletedAt(Instant.parse("2026-10-26T13:00:00Z"));
        repository.save(borrada);

        assertThat(repository.findByReservaIdAndDeletedAtIsNull(5L)).isPresent();
        assertThat(repository.findByReservaIdAndDeletedAtIsNull(6L)).isEmpty();
        assertThat(repository.findByReservaIdAndDeletedAtIsNull(99L)).isEmpty();
    }

    @Test
    void buscarPorVariasReservas_devuelveSoloLasQueTienenClaseYNoLasBorradas() {
        guardar(1L, instructor);
        guardar(2L, instructor);
        Clase borrada = guardar(3L, instructor);
        borrada.setDeletedAt(Instant.parse("2026-10-26T13:00:00Z"));
        repository.save(borrada);
        guardar(4L, instructor);

        List<Clase> clases = repository.findByReservaIdInAndDeletedAtIsNull(List.of(1L, 3L, 5L, 4L));

        assertThat(clases).extracting(Clase::getReservaId).containsExactlyInAnyOrder(1L, 4L);
        assertThat(repository.findByReservaIdInAndDeletedAtIsNull(List.of())).isEmpty();
    }

    @Test
    void unaSegundaClaseParaLaMismaReserva_laRechazaElIndiceUnico() {
        guardar(1L, instructor);

        assertThatThrownBy(() -> guardar(1L, instructor)).isInstanceOf(DuplicateKeyException.class);
    }
}
