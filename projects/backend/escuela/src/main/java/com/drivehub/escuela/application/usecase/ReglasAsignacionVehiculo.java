package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.InstructorAsignadoOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

/**
 * Reglas que comparten asignar, cambiar de instructor y liberar un auto (CU10).
 */
final class ReglasAsignacionVehiculo {

    private static final Logger LOG = LoggerFactory.getLogger(ReglasAsignacionVehiculo.class);
    static final String MODULO = "asignaciones-vehiculo";

    private ReglasAsignacionVehiculo() {
    }

    static Vehiculo vehiculo(VehiculoRepository repositorio, UUID escuelaCodigo, UUID vehiculoCodigo) {
        return repositorio.buscarPorCodigo(escuelaCodigo, vehiculoCodigo).orElseThrow(() -> {
            LOG.warn("Asignación rechazada: el auto no existe", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO), kv("vehiculoCodigo", vehiculoCodigo));
            return new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe");
        });
    }

    static Instructor instructor(InstructorRepository repositorio, UUID escuelaCodigo, UUID instructorCodigo) {
        return repositorio.buscarPorCodigo(escuelaCodigo, instructorCodigo).orElseThrow(() -> {
            LOG.warn("Asignación rechazada: el instructor no existe", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO), kv("instructorCodigo", instructorCodigo));
            return new NotFoundException(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe");
        });
    }

    static void vehiculoDisponible(Vehiculo vehiculo) {
        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            LOG.warn("Asignación rechazada: el auto no está disponible", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.VEHICULO_NO_DISPONIBLE), kv("vehiculoCodigo", vehiculo.getCodigo()),
                kv("estado", vehiculo.getEstado()));
            throw new ConflictException(EscuelaErrorCodes.VEHICULO_NO_DISPONIBLE,
                "Solo se asigna un auto disponible: este está " + vehiculo.getEstado());
        }
    }

    static void instructorActivo(Instructor instructor) {
        if (instructor.getEstado() != EstadoInstructor.ACTIVO) {
            LOG.warn("Asignación rechazada: el instructor no está activo", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.INSTRUCTOR_NO_ACTIVO),
                kv("instructorCodigo", instructor.getCodigo()), kv("estado", instructor.getEstado()));
            throw new ConflictException(EscuelaErrorCodes.INSTRUCTOR_NO_ACTIVO,
                "Solo se asigna un auto a un instructor activo");
        }
    }

    static ConflictException yaAsignado(UUID vehiculoCodigo, String mensaje) {
        LOG.warn("Asignación rechazada: el auto ya está asignado", kv("module", MODULO),
            kv("errorCode", EscuelaErrorCodes.VEHICULO_YA_ASIGNADO), kv("vehiculoCodigo", vehiculoCodigo));
        return new ConflictException(EscuelaErrorCodes.VEHICULO_YA_ASIGNADO, mensaje);
    }

    static AsignacionVehiculo exigirVigente(AsignacionVehiculo vigente, UUID vehiculoCodigo) {
        if (vigente == null) {
            LOG.warn("Asignación rechazada: el auto no tiene instructor", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.VEHICULO_SIN_ASIGNACION), kv("vehiculoCodigo", vehiculoCodigo));
            throw new ConflictException(EscuelaErrorCodes.VEHICULO_SIN_ASIGNACION,
                "El auto no tiene un instructor asignado");
        }
        return vigente;
    }

    /**
     * La fecha de un movimiento: hoy si no se indica, nunca futura y nunca anterior a `minima`
     * (el inicio de la asignación que se cierra o el fin de la última cerrada; nula = sin tope).
     */
    static LocalDate fecha(LocalDate fecha, LocalDate hoy, LocalDate minima) {
        LocalDate elegida = fecha == null ? hoy : fecha;
        if (elegida.isAfter(hoy)) {
            throw invalida("La fecha no puede ser futura");
        }
        if (minima != null && elegida.isBefore(minima)) {
            throw invalida("La fecha no puede ser anterior a " + minima);
        }
        return elegida;
    }

    private static ValidationException invalida(String mensaje) {
        LOG.warn("Asignación rechazada: fecha inválida", kv("module", MODULO),
            kv("errorCode", EscuelaErrorCodes.VALIDACION));
        return new ValidationException(EscuelaErrorCodes.VALIDACION, mensaje);
    }

    /** El auto con su instructor; el nombre no se pide a Identidad en las acciones. */
    static AsignacionVehiculoOutput aSalida(Vehiculo v, AsignacionVehiculo vigente) {
        return new AsignacionVehiculoOutput(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getEstado(),
            vigente == null ? null : new InstructorAsignadoOutput(vigente.getInstructorCodigo(), null),
            vigente == null ? null : vigente.getFechaInicio());
    }
}
