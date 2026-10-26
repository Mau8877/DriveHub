package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.CambiarAsignacionInput;
import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU10: el Administrador de escuela asigna un auto disponible, que todavía no tiene instructor, a un
 * instructor activo de SU escuela.
 *
 * * No cambia el estado del auto ni el del instructor.
 */
@Service
@RequiredArgsConstructor
public class AsignarVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AsignarVehiculoUseCase.class);

    private final AsignacionVehiculoRepository asignacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final InstructorRepository instructorRepository;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public AsignacionVehiculoOutput ejecutar(CambiarAsignacionInput input) {
        Vehiculo vehiculo = ReglasAsignacionVehiculo.vehiculo(
            vehiculoRepository, input.escuelaCodigo(), input.vehiculoCodigo());
        Instructor instructor = ReglasAsignacionVehiculo.instructor(
            instructorRepository, input.escuelaCodigo(), input.instructorCodigo());

        if (asignacionRepository.buscarVigente(input.escuelaCodigo(), input.vehiculoCodigo()).isPresent()) {
            throw ReglasAsignacionVehiculo.yaAsignado(input.vehiculoCodigo(),
                "El auto ya tiene un instructor asignado; para cambiarlo usa \"Cambiar instructor\"");
        }
        ReglasAsignacionVehiculo.vehiculoDisponible(vehiculo);
        ReglasAsignacionVehiculo.instructorActivo(instructor);

        LocalDate minima = asignacionRepository.ultimaFechaFin(input.escuelaCodigo(), input.vehiculoCodigo())
            .orElse(null);
        LocalDate inicio = ReglasAsignacionVehiculo.fecha(input.fecha(), LocalDate.now(reloj), minima);

        AsignacionVehiculo creada = asignacionRepository.abrir(input.escuelaCodigo(), AsignacionVehiculo.builder()
            .codigo(UuidV7.generar())
            .vehiculoCodigo(vehiculo.getCodigo())
            .instructorCodigo(instructor.getCodigo())
            .fechaInicio(inicio)
            .build());
        LOG.info("Auto asignado a un instructor", kv("module", ReglasAsignacionVehiculo.MODULO),
            kv("vehiculoCodigo", vehiculo.getCodigo()), kv("instructorCodigo", instructor.getCodigo()),
            kv("fechaInicio", inicio));
        return ReglasAsignacionVehiculo.aSalida(vehiculo, creada);
    }
}
