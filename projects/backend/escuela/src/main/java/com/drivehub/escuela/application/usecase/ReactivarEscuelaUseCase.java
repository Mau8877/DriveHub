package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU05: el Super admin reactiva una escuela desactivada.
 *
 * * No llama a Identidad: al quitarse el borrado lógico, todas las cuentas de la escuela
 * * vuelven a poder iniciar sesión con sus mismas credenciales, sin perder nada.
 */
@Service
@RequiredArgsConstructor
public class ReactivarEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReactivarEscuelaUseCase.class);
    private static final String MODULO = "escuelas";

    private final EscuelaRepository escuelaRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public EscuelaOutput ejecutar(UUID codigo) {
        Escuela escuela = escuelaRepository.buscarPorCodigo(codigo).orElseThrow(() -> {
            LOG.warn("Reactivación rechazada: la escuela no existe", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO), kv("escuelaCodigo", codigo));
            return new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "La escuela no existe");
        });
        // * Si dos peticiones llegan a la vez, la segunda no cambia nada y también se rechaza
        if (escuela.activa() || !escuelaRepository.reactivar(codigo)) {
            LOG.warn("Reactivación rechazada: la escuela ya estaba activa", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.ESCUELA_YA_ACTIVA), kv("escuelaCodigo", codigo));
            throw new ConflictException(EscuelaErrorCodes.ESCUELA_YA_ACTIVA, "La escuela ya estaba activa");
        }
        LOG.info("Escuela reactivada", kv("module", MODULO), kv("escuelaCodigo", codigo));
        Escuela reactivada = escuelaRepository.buscarPorCodigo(codigo).orElseThrow();
        return ListarEscuelasUseCase.aSalida(reactivada);
    }
}
