package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU05: el Super admin desactiva una escuela (borrado lógico, reversible).
 *
 * * No llama a Identidad ni toca las cuentas: Identidad ya comprueba la escuela al iniciar y
 * * al renovar la sesión, así que TODAS las cuentas de una escuela inactiva (Administrador,
 * * Instructores y Estudiantes) quedan sin acceso, y lo recuperan al reactivarla.
 */
@Service
@RequiredArgsConstructor
public class DesactivarEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(DesactivarEscuelaUseCase.class);
    private static final String MODULO = "escuelas";

    private final EscuelaRepository escuelaRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public void ejecutar(UUID codigo) {
        Escuela escuela = escuelaRepository.buscarPorCodigo(codigo).orElseThrow(() -> {
            LOG.warn("Desactivación rechazada: la escuela no existe", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO), kv("escuelaCodigo", codigo));
            return new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "La escuela no existe");
        });
        // * Si dos peticiones llegan a la vez, la segunda no cambia nada y también se rechaza
        if (!escuela.activa() || !escuelaRepository.desactivar(codigo)) {
            LOG.warn("Desactivación rechazada: la escuela ya estaba inactiva", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.ESCUELA_YA_INACTIVA), kv("escuelaCodigo", codigo));
            throw new ConflictException(EscuelaErrorCodes.ESCUELA_YA_INACTIVA, "La escuela ya estaba inactiva");
        }
        LOG.info("Escuela desactivada", kv("module", MODULO), kv("escuelaCodigo", codigo));
    }
}
