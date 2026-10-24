package com.drivehub.escuela.infrastructure.config;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ficha del instructor de ejemplo para desarrollo: la de `instructorexample@gmail.com`.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * La cuenta la siembra Identidad (IdentidadSeeder) con un codigo fijo, el mismo que usa esta ficha
 * * como `usuario_codigo`: así quedan enlazadas sin llamarse entre sí y el instructor aparece en el
 * * listado de la escuela de ejemplo.
 * * Se siembra por número de licencia: si ya existe, no se toca. El vencimiento se calcula con el
 * * reloj para que la licencia de ejemplo nunca esté vencida.
 */
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class InstructorSeeder implements ApplicationRunner {

    public static final UUID CODIGO_USUARIO_INSTRUCTOR_EXAMPLE =
        UUID.fromString("0198f0a0-0000-7000-8000-000000000002");
    public static final String NRO_LICENCIA_EXAMPLE = "SEED-LIC-001";
    public static final List<String> CATEGORIAS_EXAMPLE = List.of("P", "B");

    private static final Logger LOG = LoggerFactory.getLogger(InstructorSeeder.class);
    private static final int ANIOS_DE_VIGENCIA = 3;

    private final InstructorRepository instructorRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;
    private final Clock reloj;

    @Override
    public void run(ApplicationArguments args) {
        if (instructorRepository.existePorNroLicencia(NRO_LICENCIA_EXAMPLE)) {
            return;
        }
        List<CategoriaLicencia> categorias = categoriaLicenciaRepository.buscarPorSiglas(CATEGORIAS_EXAMPLE);
        if (categorias.isEmpty()) {
            LOG.warn("No se sembró la ficha del instructor de ejemplo: las categorías de licencia no existen");
            return;
        }
        LocalDate hoy = LocalDate.now(reloj);
        instructorRepository.guardar(Instructor.nuevo(
            UuidV7.generar(), CODIGO_USUARIO_INSTRUCTOR_EXAMPLE, EscuelaSeeder.CODIGO_ESCUELA_EXAMPLE,
            NRO_LICENCIA_EXAMPLE, hoy.plusYears(ANIOS_DE_VIGENCIA), hoy, new HashSet<>(categorias), hoy));
        LOG.info("Ficha del instructor de ejemplo creada para desarrollo");
    }
}
