package com.drivehub.escuela.application.usecase;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Devuelve los datos públicos de una escuela activa (CU01).
 *
 * * Una escuela inexistente y una inactiva responden igual (ERR_ESC_01) para
 * * no revelar qué escuelas existen.
 */
@Service
@RequiredArgsConstructor
public class ObtenerEscuelaPorSlugUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObtenerEscuelaPorSlugUseCase.class);

    private final EscuelaRepository escuelaRepository;

    @Transactional(readOnly = true)
    public EscuelaPublicaOutput ejecutar(String slug) {
        String slugNormalizado = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);

        Escuela escuela = escuelaRepository.buscarActivaPorSlug(slugNormalizado)
            .orElseThrow(() -> {
                MDC.put("module", "escuelas");
                MDC.put("errorCode", EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA);
                try {
                    LOG.warn("Escuela no encontrada o inactiva");
                } finally {
                    MDC.remove("module");
                    MDC.remove("errorCode");
                }
                return new NotFoundException(
                    EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA, "La escuela no existe o no está disponible");
            });

        return new EscuelaPublicaOutput(escuela.getCodigo(), escuela.getSlug(), escuela.getNombre());
    }
}
