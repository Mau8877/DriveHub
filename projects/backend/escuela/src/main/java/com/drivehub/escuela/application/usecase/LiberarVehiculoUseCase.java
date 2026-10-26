package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU10: el Administrador de escuela libera un auto asignado: cierra su asignación vigente y el auto
 * queda sin instructor. Se puede liberar un auto en cualquier estado (también uno de baja).
 */
@Service
@RequiredArgsConstructor
public class LiberarVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(LiberarVehiculoUseCase.class);

    private final AsignacionVehiculoRepository asignacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public AsignacionVehiculoOutput ejecutar(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fecha) {
        Vehiculo vehiculo = ReglasAsignacionVehiculo.vehiculo(vehiculoRepository, escuelaCodigo, vehiculoCodigo);
        AsignacionVehiculo vigente = ReglasAsignacionVehiculo.exigirVigente(
            asignacionRepository.buscarVigente(escuelaCodigo, vehiculoCodigo).orElse(null), vehiculoCodigo);

        LocalDate fin = ReglasAsignacionVehiculo.fecha(fecha, LocalDate.now(reloj), vigente.getFechaInicio());
        asignacionRepository.cerrar(escuelaCodigo, vehiculoCodigo, fin);
        LOG.info("Auto liberado", kv("module", ReglasAsignacionVehiculo.MODULO), kv("vehiculoCodigo", vehiculoCodigo),
            kv("instructorCodigo", vigente.getInstructorCodigo()), kv("fechaFin", fin));
        return ReglasAsignacionVehiculo.aSalida(vehiculo, null);
    }
}
