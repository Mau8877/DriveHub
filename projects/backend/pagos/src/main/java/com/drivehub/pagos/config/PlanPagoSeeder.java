package com.drivehub.pagos.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Planes de horas de ejemplo para desarrollo: los de `escuelaexample`, para comprar 1, 5, 7, 10 y 15 horas.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * El codigo de la escuela es el fijo que siembran Escuela e Identidad (EscuelaSeeder, IdentidadSeeder):
 * * Pagos no llama a ningún servicio para saberlo.
 * * Los precios bajan por hora cuanto más horas se compran (60 Bs la hora suelta, 46,67 Bs en el plan de 15).
 * * Se siembra por nombre dentro de la escuela: si ya existe, no se toca. Se crean de más horas a menos para
 * * que el listado (del más reciente al más antiguo) los muestre de menos a más horas.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class PlanPagoSeeder implements ApplicationRunner {

    public static final UUID CODIGO_ESCUELA_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");

    private static final Logger LOG = LoggerFactory.getLogger(PlanPagoSeeder.class);

    private record Ejemplo(String nombre, int horas, String costoBs) {
    }

    private static final List<Ejemplo> EJEMPLOS = List.of(
        new Ejemplo("Plan 15 horas", 15, "700.00"),
        new Ejemplo("Plan 10 horas", 10, "500.00"),
        new Ejemplo("Plan 7 horas", 7, "370.00"),
        new Ejemplo("Plan 5 horas", 5, "275.00"),
        new Ejemplo("Clase suelta (1 hora)", 1, "60.00"));

    private final PlanPagoRepository planRepository;

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;
        for (Ejemplo e : EJEMPLOS) {
            if (planRepository.existsByEscuelaCodigoAndNombreIgnoreCase(CODIGO_ESCUELA_EXAMPLE, e.nombre())) {
                continue;
            }
            PlanPago plan = new PlanPago(CODIGO_ESCUELA_EXAMPLE, e.nombre(), (short) e.horas(),
                new BigDecimal(e.costoBs()));
            planRepository.saveAndFlush(plan);
            creados++;
        }
        if (creados > 0) {
            LOG.info("Planes de horas de ejemplo creados para desarrollo ({})", creados);
        }
    }
}
