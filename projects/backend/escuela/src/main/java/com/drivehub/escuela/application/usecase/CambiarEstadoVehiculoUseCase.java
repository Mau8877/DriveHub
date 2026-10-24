package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU07: el Administrador de escuela envía un auto a mantenimiento, lo habilita de nuevo o lo da
 * de baja. Un solo caso de uso con el estado destino como parámetro: las tres acciones son la
 * misma regla (EstadoVehiculo.permiteIrA).
 */
@Service
@RequiredArgsConstructor
public class CambiarEstadoVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CambiarEstadoVehiculoUseCase.class);

    private final VehiculoRepository vehiculoRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public VehiculoOutput ejecutar(UUID escuelaCodigo, UUID codigo, EstadoVehiculo destino) {
        Vehiculo actual = vehiculoRepository.buscarPorCodigo(escuelaCodigo, codigo).orElseThrow(() -> {
            LOG.warn("Cambio de estado rechazado: el auto no existe", kv("module", ReglasVehiculo.MODULO),
                kv("errorCode", EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO), kv("vehiculoCodigo", codigo));
            return new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe");
        });
        if (!actual.getEstado().permiteIrA(destino)) {
            LOG.warn("Cambio de estado rechazado: transición no permitida", kv("module", ReglasVehiculo.MODULO),
                kv("errorCode", EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA), kv("vehiculoCodigo", codigo),
                kv("desde", actual.getEstado()), kv("hacia", destino));
            throw new ConflictException(EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA,
                "Un auto " + actual.getEstado() + " no puede pasar a " + destino);
        }

        Vehiculo guardado = vehiculoRepository.actualizar(actual.toBuilder().estado(destino).build()).orElseThrow(() ->
            new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe"));
        LOG.info("Estado del auto cambiado", kv("module", ReglasVehiculo.MODULO), kv("vehiculoCodigo", codigo),
            kv("desde", actual.getEstado()), kv("hacia", destino));
        return ReglasVehiculo.aSalida(guardado);
    }
}
