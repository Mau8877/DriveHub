package com.drivehub.escuela.infrastructure.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;
import com.drivehub.escuela.infrastructure.persistence.repository.EscuelaJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Datos de ejemplo para desarrollo: la escuela `escuelaexample`.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * El codigo es fijo porque el seeder de Identidad (adminexample) lo usa
 * * como escuela_codigo sin llamar a este servicio.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class EscuelaSeeder implements ApplicationRunner {

    public static final UUID CODIGO_ESCUELA_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");
    public static final String SLUG_ESCUELA_EXAMPLE = "escuelaexample";

    private static final Logger LOG = LoggerFactory.getLogger(EscuelaSeeder.class);

    private final EscuelaJpaRepository escuelaJpaRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (escuelaJpaRepository.existePorSlugIncluyendoEliminadas(SLUG_ESCUELA_EXAMPLE)) {
            return;
        }
        escuelaJpaRepository.save(new EscuelaModel(
            CODIGO_ESCUELA_EXAMPLE, SLUG_ESCUELA_EXAMPLE, "Escuela Example"));
        LOG.info("Escuela de ejemplo creada para desarrollo");
    }
}
