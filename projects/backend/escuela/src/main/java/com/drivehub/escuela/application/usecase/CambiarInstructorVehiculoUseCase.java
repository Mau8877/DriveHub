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
 * CU10: el Administrador de escuela cambia un auto asignado a otro instructor activo de SU escuela.
 * Cierra la asignación vigente con esa fecha y abre la nueva, las dos juntas: si algo falla, el auto
 * sigue con su instructor anterior.
 */
@Service
@RequiredArgsConstructor
public class CambiarInstructorVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CambiarInstructorVehiculoUseCase.class);

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

        AsignacionVehiculo vigente = ReglasAsignacionVehiculo.exigirVigente(
            asignacionRepository.buscarVigente(input.escuelaCodigo(), input.vehiculoCodigo()).orElse(null),
            input.vehiculoCodigo());
        if (vigente.getInstructorCodigo().equals(instructor.getCodigo())) {
            throw ReglasAsignacionVehiculo.yaAsignado(input.vehiculoCodigo(),
                "El auto ya está asignado a ese instructor");
        }
        ReglasAsignacionVehiculo.vehiculoDisponible(vehiculo);
        ReglasAsignacionVehiculo.instructorActivo(instructor);

        LocalDate fecha = ReglasAsignacionVehiculo.fecha(
            input.fecha(), LocalDate.now(reloj), vigente.getFechaInicio());

        AsignacionVehiculo nueva = asignacionRepository.cerrarYAbrir(input.escuelaCodigo(), vehiculo.getCodigo(),
            fecha, AsignacionVehiculo.builder()
                .codigo(UuidV7.generar())
                .vehiculoCodigo(vehiculo.getCodigo())
                .instructorCodigo(instructor.getCodigo())
                .fechaInicio(fecha)
                .build());
        LOG.info("Auto cambiado a otro instructor", kv("module", ReglasAsignacionVehiculo.MODULO),
            kv("vehiculoCodigo", vehiculo.getCodigo()), kv("desdeInstructorCodigo", vigente.getInstructorCodigo()),
            kv("haciaInstructorCodigo", instructor.getCodigo()), kv("fecha", fecha));
        return ReglasAsignacionVehiculo.aSalida(vehiculo, nueva);
    }
}
