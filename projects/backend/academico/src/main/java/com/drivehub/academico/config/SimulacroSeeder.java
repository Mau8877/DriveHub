package com.drivehub.academico.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.simulacros.model.ReglaSimulacro;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Simulacros de ejemplo para desarrollo: dos configuraciones de `escuelaexample` que usan los temas de las
 * preguntas que siembra {@link PreguntaSeeder}.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * Se siembra por nombre: si ya existe, no se toca.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SimulacroSeeder implements ApplicationRunner {

    private static final UUID AUTOR_EJEMPLO = UUID.fromString("0198f0a0-0000-7000-8000-0000000000aa");

    private static final Logger LOG = LoggerFactory.getLogger(SimulacroSeeder.class);

    private record Ejemplo(String nombre, String categoria, int duracionMin, double puntajeAprobacion,
            List<ReglaSimulacro> reglas) {
    }

    private static final List<Ejemplo> EJEMPLOS = List.of(
        new Ejemplo("Simulacro general de teoría", "GENERAL", 20, 70,
            List.of(new ReglaSimulacro("Señales", 3), new ReglaSimulacro("Prioridad de paso", 2))),
        new Ejemplo("Simulacro categoría B", "B", 30, 80,
            List.of(new ReglaSimulacro("Seguridad vial", 3), new ReglaSimulacro("Mecánica básica", 1))));

    private final SimulacroConfigRepository simulacroRepository;

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;
        for (Ejemplo e : EJEMPLOS) {
            if (simulacroRepository.existsByEscuelaCodigoAndNombre(PreguntaSeeder.CODIGO_ESCUELA_EXAMPLE, e.nombre())) {
                continue;
            }
            SimulacroConfig simulacro = new SimulacroConfig();
            simulacro.setEscuelaCodigo(PreguntaSeeder.CODIGO_ESCUELA_EXAMPLE);
            simulacro.setCreadoPorCodigo(AUTOR_EJEMPLO);
            simulacro.setNombre(e.nombre());
            simulacro.setCategoria(e.categoria());
            simulacro.setDuracionMin(e.duracionMin());
            simulacro.setNroPreguntas(e.reglas().stream().mapToInt(ReglaSimulacro::cantidad).sum());
            simulacro.setPuntajeAprobacion(e.puntajeAprobacion());
            simulacro.setReglas(e.reglas());
            simulacro.setActivo(true);
            simulacroRepository.save(simulacro);
            creados++;
        }
        if (creados > 0) {
            LOG.info("Simulacros de ejemplo creados para desarrollo ({})", creados);
        }
    }
}
