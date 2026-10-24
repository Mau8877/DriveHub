package com.drivehub.academico.features.horarios.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class PlantillaHorarioRepositoryTest extends MongoIntegrationTestBase {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Autowired
    private PlantillaHorarioRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final UUID instructor = UUID.randomUUID();
    private final UUID otroInstructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        mongoTemplate.remove(new Query(), PlantillaHorario.class);
    }

    private PlantillaHorario guardar(UUID propietario, List<Integer> dias, String inicio, String fin,
            EstadoPlantilla estado) {
        PlantillaHorario horario = new PlantillaHorario(escuela, propietario, "Horario", dias, inicio, fin, 60,
            HOY, null);
        horario.setEstado(estado);
        return repository.save(horario);
    }

    @Test
    void save_asignaIdYCodigoV7YGuardaLosDias() {
        PlantillaHorario guardada = guardar(instructor, List.of(1, 3, 5), "08:00", "12:00", EstadoPlantilla.ACTIVA);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getCodigoPlantillaHorario().version()).isEqualTo(7);
        assertThat(guardada.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        PlantillaHorario leida = repository.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getDiasSemana()).containsExactly(1, 3, 5);
        assertThat(leida.getEstado()).isEqualTo(EstadoPlantilla.ACTIVA);
    }

    @Test
    void buscarPorCodigoEInstructor_devuelveElHorarioSoloSiEsDeEseInstructor() {
        PlantillaHorario horario = guardar(instructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);

        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).isPresent();
        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), otroInstructor)).isEmpty();
        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            UUID.randomUUID(), instructor)).isEmpty();
    }

    @Test
    void listarPorEstados_devuelveSoloLosPedidosDeEseInstructor() {
        guardar(instructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);
        guardar(instructor, List.of(2), "08:00", "12:00", EstadoPlantilla.PAUSADA);
        guardar(instructor, List.of(3), "08:00", "12:00", EstadoPlantilla.INACTIVA);
        guardar(otroInstructor, List.of(4), "08:00", "12:00", EstadoPlantilla.ACTIVA);

        var activosYPausados = repository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructor,
            EnumSet.of(EstadoPlantilla.ACTIVA, EstadoPlantilla.PAUSADA));
        var soloInactivos = repository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructor,
            EnumSet.of(EstadoPlantilla.INACTIVA));

        assertThat(activosYPausados).extracting(h -> h.getDiasSemana().get(0)).containsExactlyInAnyOrder(1, 2);
        assertThat(soloInactivos).extracting(h -> h.getDiasSemana().get(0)).containsExactly(3);
    }

    @Test
    void listarActivasDelInstructor_soloLasActivasDeEseInstructor() {
        guardar(instructor, List.of(1, 2), "08:00", "12:00", EstadoPlantilla.ACTIVA);
        guardar(instructor, List.of(1), "14:00", "18:00", EstadoPlantilla.PAUSADA);
        guardar(instructor, List.of(2), "14:00", "18:00", EstadoPlantilla.INACTIVA);
        guardar(otroInstructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);

        var lista = repository.findByInstructorCodigoAndEstadoAndDeletedAtIsNull(instructor,
            EstadoPlantilla.ACTIVA);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getDiasSemana()).containsExactly(1, 2);
    }

    @Test
    void listarPorEstadoDeTodosLosInstructores_paginado() {
        guardar(instructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);
        guardar(otroInstructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);
        guardar(otroInstructor, List.of(2), "08:00", "12:00", EstadoPlantilla.PAUSADA);

        var pagina = repository.findByEstadoAndDeletedAtIsNull(EstadoPlantilla.ACTIVA,
            PageRequest.of(0, 10, Sort.by("id")));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void lasFilasConDeletedAt_noAparecenEnNingunaConsulta() {
        PlantillaHorario borrada = guardar(instructor, List.of(1), "08:00", "12:00", EstadoPlantilla.ACTIVA);
        borrada.setDeletedAt(Instant.now());
        repository.save(borrada);

        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            borrada.getCodigoPlantillaHorario(), instructor)).isEmpty();
        assertThat(repository.findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(instructor,
            EnumSet.allOf(EstadoPlantilla.class))).isEmpty();
        assertThat(repository.findByInstructorCodigoAndEstadoAndDeletedAtIsNull(instructor,
            EstadoPlantilla.ACTIVA)).isEmpty();
        assertThat(repository.findByEstadoAndDeletedAtIsNull(EstadoPlantilla.ACTIVA, PageRequest.of(0, 10))
            .getTotalElements()).isZero();
    }
}
