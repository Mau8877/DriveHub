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
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU05: el Super admin cambia el nombre de una escuela activa.
 *
 * * Es lo único editable: el slug ya viaja en enlaces de registro compartidos, y el
 * * Administrador no se toca desde este caso de uso. Por eso no hay parámetro para ellos.
 */
@Service
@RequiredArgsConstructor
public class EditarEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarEscuelaUseCase.class);
    private static final String MODULO = "escuelas";
    private static final int NOMBRE_MIN = 3;
    private static final int NOMBRE_MAX = 120;

    private final EscuelaRepository escuelaRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public EscuelaOutput ejecutar(UUID codigo, String nombre) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.length() < NOMBRE_MIN || nombreLimpio.length() > NOMBRE_MAX) {
            LOG.warn("Edición rechazada: nombre inválido",
                kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.VALIDACION));
            throw new ValidationException(EscuelaErrorCodes.VALIDACION,
                "El nombre debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX + " caracteres");
        }

        Escuela escuela = escuelaRepository.buscarPorCodigo(codigo).orElseThrow(() -> {
            LOG.warn("Edición rechazada: la escuela no existe", kv("module", MODULO),
                kv("errorCode", EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO), kv("escuelaCodigo", codigo));
            return new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "La escuela no existe");
        });
        if (!escuela.activa()) {
            throw inactiva(codigo);
        }

        Escuela actualizada = escuelaRepository.actualizarNombre(codigo, nombreLimpio)
            .orElseThrow(() -> inactiva(codigo));
        LOG.info("Escuela editada", kv("module", MODULO), kv("escuelaCodigo", codigo));
        return ListarEscuelasUseCase.aSalida(actualizada);
    }

    private ConflictException inactiva(UUID codigo) {
        LOG.warn("Edición rechazada: la escuela está inactiva", kv("module", MODULO),
            kv("errorCode", EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE), kv("escuelaCodigo", codigo));
        return new ConflictException(EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE,
            "La escuela está inactiva: reactívala antes de editarla");
    }
}
