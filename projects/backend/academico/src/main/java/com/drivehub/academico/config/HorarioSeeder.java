package com.drivehub.academico.config;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.repository.PlantillaHorarioRepository;
import com.drivehub.academico.features.horarios.service.FranjaGeneracionService;

import lombok.RequiredArgsConstructor;

/**
 * Horario de ejemplo para desarrollo: el del instructor `instructorexample@gmail.com`, de lunes a viernes de
 * 08:00 a 12:00 en clases de una hora, con sus franjas generadas.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * Los codigos de la escuela y de la persona son los fijos que siembran Escuela e Identidad (EscuelaSeeder,
 * * IdentidadSeeder): Académico no llama a ningún servicio para saberlos.
 * * Las franjas las genera el mismo servicio que usa un horario creado a mano (con el horizonte del servicio) y,
 * * como esa generación es idempotente, el job diario las sigue extendiendo.
 * * Se siembra solo si el instructor no tiene ningún horario (en cualquier estado): uno pausado o desactivado a
 * * mano no se vuelve a crear en cada arranque.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class HorarioSeeder implements ApplicationRunner {

    public static final UUID CODIGO_ESCUELA_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");
    public static final UUID CODIGO_INSTRUCTOR_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000002");
    public static final String NOMBRE = "Horario de ejemplo";
    public static final List<Integer> DIAS = List.of(1, 2, 3, 4, 5);
    public static final String HORA_INICIO = "08:00";
    public static final String HORA_FIN = "12:00";
    public static final int DURACION_CLASE_MIN = 60;

    private static final Logger LOG = LoggerFactory.getLogger(HorarioSeeder.class);

    private final PlantillaHorarioRepository plantillaRepository;
    private final FranjaGeneracionService generacion;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        boolean yaTieneHorario = !plantillaRepository
            .findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(CODIGO_INSTRUCTOR_EXAMPLE,
                List.of(EstadoPlantilla.values()))
            .isEmpty();
        if (yaTieneHorario) {
            return;
        }
        PlantillaHorario horario = plantillaRepository.save(new PlantillaHorario(CODIGO_ESCUELA_EXAMPLE,
            CODIGO_INSTRUCTOR_EXAMPLE, NOMBRE, DIAS, HORA_INICIO, HORA_FIN, DURACION_CLASE_MIN,
            LocalDate.now(clock), null));
        int franjas = generacion.generar(horario);
        LOG.info("Horario de ejemplo creado para desarrollo ({} franjas)", franjas);
    }
}
