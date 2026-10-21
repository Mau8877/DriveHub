package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.EscuelaEstadoOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Devuelve una escuela y si está activa, en cualquier estado (CU02).
 *
 * * A diferencia de ObtenerEscuelaPorSlugUseCase (registro, solo activas),
 * * una escuela suspendida SÍ se devuelve, con activa=false: Identidad
 * * necesita distinguir "suspendida" de "no existe".
 */
@Service
@RequiredArgsConstructor
public class ObtenerEscuelaPorCodigoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObtenerEscuelaPorCodigoUseCase.class);

    private final EscuelaRepository escuelaRepository;

    @Transactional(readOnly = true)
    public EscuelaEstadoOutput ejecutar(UUID codigo) {
        Escuela escuela = escuelaRepository.buscarPorCodigo(codigo)
            .orElseThrow(() -> {
                LOG.warn("Escuela no encontrada por codigo",
                    kv("module", "escuelas"), kv("errorCode", EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO));
                return new NotFoundException(
                    EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "La escuela no existe");
            });

        return new EscuelaEstadoOutput(
            escuela.getCodigo(), escuela.getSlug(), escuela.getNombre(),
            escuela.activa());
    }
}
