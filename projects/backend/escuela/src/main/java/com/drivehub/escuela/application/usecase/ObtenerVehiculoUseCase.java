package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU07: un auto de la escuela, en cualquier estado (la pantalla de edición lo carga con esto).
 * Uno de otra escuela es, para quien lo pide, uno que no existe.
 */
@Service
@RequiredArgsConstructor
public class ObtenerVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObtenerVehiculoUseCase.class);

    private final VehiculoRepository vehiculoRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public VehiculoOutput ejecutar(UUID escuelaCodigo, UUID codigo) {
        return vehiculoRepository.buscarPorCodigo(escuelaCodigo, codigo)
            .map(ReglasVehiculo::aSalida)
            .orElseThrow(() -> {
                LOG.warn("Consulta rechazada: el auto no existe", kv("module", ReglasVehiculo.MODULO),
                    kv("errorCode", EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO), kv("vehiculoCodigo", codigo));
                return new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe");
            });
    }
}
