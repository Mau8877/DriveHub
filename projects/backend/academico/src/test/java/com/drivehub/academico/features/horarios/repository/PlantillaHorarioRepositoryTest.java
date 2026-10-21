package com.drivehub.academico.features.horarios.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;
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

    private PlantillaHorario guardar(UUID propietario, int dia, String inicio, String fin, boolean activa) {
        PlantillaHorario horario = new PlantillaHorario(escuela, propietario, dia, inicio, fin, 60, HOY, null);
        horario.setActiva(activa);
        return repository.save(horario);
    }

    @Test
    void save_asignaIdYCodigoV7() {
        PlantillaHorario guardada = guardar(instructor, 1, "08:00", "12:00", true);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getCodigoPlantillaHorario().version()).isEqualTo(7);
        assertThat(guardada.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void buscarPorCodigoEInstructor_devuelveElHorarioSoloSiEsDeEseInstructor() {
        PlantillaHorario horario = guardar(instructor, 1, "08:00", "12:00", true);

        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), instructor)).isPresent();
        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            horario.getCodigoPlantillaHorario(), otroInstructor)).isEmpty();
        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            UUID.randomUUID(), instructor)).isEmpty();
    }

    @Test
    void listarPorInstructor_ordenadoYPaginado() {
        guardar(instructor, 3, "08:00", "12:00", true);
        guardar(instructor, 1, "14:00", "18:00", true);
        guardar(instructor, 1, "08:00", "12:00", true);
        guardar(otroInstructor, 2, "08:00", "12:00", true);

        var pagina = repository.findByInstructorCodigoAndDeletedAtIsNull(instructor,
            PageRequest.of(0, 2, Sort.by("diaSemana", "horaInicio")));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(PlantillaHorario::getDiaSemana, PlantillaHorario::getHoraInicio)
            .containsExactly(tuple(1, "08:00"),
                tuple(1, "14:00"));
    }

    @Test
    void listarConFiltroDeActiva_separaActivosDeInactivos() {
        guardar(instructor, 1, "08:00", "12:00", true);
        guardar(instructor, 2, "08:00", "12:00", false);

        var activos = repository.findByInstructorCodigoAndActivaAndDeletedAtIsNull(instructor, true,
            PageRequest.of(0, 20));
        var inactivos = repository.findByInstructorCodigoAndActivaAndDeletedAtIsNull(instructor, false,
            PageRequest.of(0, 20));

        assertThat(activos.getContent()).extracting(PlantillaHorario::getDiaSemana).containsExactly(1);
        assertThat(inactivos.getContent()).extracting(PlantillaHorario::getDiaSemana).containsExactly(2);
    }

    @Test
    void listarActivasDeUnDia_soloLasActivasDeEseInstructorYDia() {
        guardar(instructor, 1, "08:00", "12:00", true);
        guardar(instructor, 1, "14:00", "18:00", false);
        guardar(instructor, 2, "08:00", "12:00", true);
        guardar(otroInstructor, 1, "08:00", "12:00", true);

        var lista = repository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getHoraInicio()).isEqualTo("08:00");
    }

    @Test
    void listarTodasLasActivas_incluyeDeTodosLosInstructores() {
        guardar(instructor, 1, "08:00", "12:00", true);
        guardar(otroInstructor, 1, "08:00", "12:00", true);
        guardar(otroInstructor, 2, "08:00", "12:00", false);

        var pagina = repository.findByActivaTrueAndDeletedAtIsNull(PageRequest.of(0, 10, Sort.by("id")));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void lasFilasConDeletedAt_noAparecenEnNingunaConsulta() {
        PlantillaHorario borrada = guardar(instructor, 1, "08:00", "12:00", true);
        borrada.setDeletedAt(Instant.now());
        repository.save(borrada);

        assertThat(repository.findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            borrada.getCodigoPlantillaHorario(), instructor)).isEmpty();
        assertThat(repository.findByInstructorCodigoAndDeletedAtIsNull(instructor, PageRequest.of(0, 10))
            .getTotalElements()).isZero();
        assertThat(repository.findByInstructorCodigoAndActivaAndDeletedAtIsNull(instructor, true,
            PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(repository.findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(instructor, 1))
            .isEmpty();
        assertThat(repository.findByActivaTrueAndDeletedAtIsNull(PageRequest.of(0, 10)).getTotalElements())
            .isZero();
    }
}
