package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.application.dto.EditarInstructorInput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.NroLicencia;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela corrige la ficha profesional y la ubicación de un instructor de
 * SU escuela. Nada de sus datos personales.
 *
 * * Valida TODO lo que llega, en Escuela, antes de tocar nada. Si llegan coordenadas distintas de las
 * * que tiene la persona, Identidad actualiza la ubicación (y recalcula la dirección) y DESPUÉS se
 * * guarda la ficha. Si guardar la ficha fallara, la ubicación nueva queda: es inofensivo y el
 * * reintento la repite sin problema, por eso no hay compensación.
 * * El vencimiento de la licencia solo se valida si cambia: dejar sin tocar una licencia ya vencida no
 * * impide corregir otros datos (por ejemplo pasarla a LICENCIA o INACTIVO).
 * * No abre una transacción propia: cada método del repositorio abre la suya (ver
 * * {@link ListarInstructoresUseCase}).
 * ! En los registros solo van los NOMBRES de los campos modificados, nunca sus valores.
 */
@Service
@RequiredArgsConstructor
public class EditarInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarInstructorUseCase.class);
    private static final String MODULO = "instructores";

    private final InstructorRepository instructorRepository;
    private final CategoriaLicenciaRepository categoriaRepository;
    private final InstructorCuentaGateway cuentaGateway;
    private final InstructorMapeador mapeador;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public InstructorDetalleOutput ejecutar(EditarInstructorInput input) {
        Instructor actual = instructorRepository.buscarPorCodigo(input.escuelaCodigo(), input.instructorCodigo())
            .orElseThrow(() -> {
                LOG.warn("Edición rechazada: instructor no encontrado en la escuela",
                    kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
                return new NotFoundException(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe");
            });

        boolean hayUbicacion = input.latitud() != null || input.longitud() != null;
        if (input.nroLicencia() == null && input.licenciaVence() == null && input.fechaContratacion() == null
                && input.categorias() == null && input.estado() == null && !hayUbicacion) {
            throw rechazo("No hay nada que cambiar");
        }
        if (hayUbicacion && (input.latitud() == null || input.longitud() == null)) {
            throw rechazo("La latitud y la longitud van juntas");
        }

        // 1. Se valida todo lo que llega, antes de tocar nada
        LocalDate hoy = LocalDate.now(reloj);
        List<String> campos = new ArrayList<>();
        Instructor.InstructorBuilder cambios = actual.toBuilder();

        if (input.nroLicencia() != null) {
            String nueva = NroLicencia.normalizarYValidar(input.nroLicencia());
            if (!nueva.equals(actual.getNroLicencia())
                    && instructorRepository.existePorNroLicenciaDeOtro(nueva, actual.getCodigo())) {
                LOG.warn("Edición rechazada: número de licencia en uso",
                    kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
                throw new ConflictException(EscuelaErrorCodes.NRO_LICENCIA_EN_USO,
                    "El número de licencia ya pertenece a otro instructor");
            }
            cambios.nroLicencia(nueva);
            campos.add("nroLicencia");
        }
        if (input.licenciaVence() != null) {
            if (!input.licenciaVence().equals(actual.getLicenciaVence())) {
                Instructor.validarVencimiento(input.licenciaVence(), hoy);
            }
            cambios.licenciaVence(input.licenciaVence());
            campos.add("licenciaVence");
        }
        if (input.fechaContratacion() != null) {
            cambios.fechaContratacion(Instructor.validarContratacion(input.fechaContratacion(), hoy));
            campos.add("fechaContratacion");
        }
        if (input.categorias() != null) {
            cambios.categorias(categoriasValidas(Instructor.validarSiglas(input.categorias())));
            campos.add("categorias");
        }
        if (input.estado() != null) {
            cambios.estado(input.estado());
            campos.add("estado");
        }

        // 2. La ubicación va a Identidad antes de guardar la ficha
        PersonaInstructor persona = null;
        if (hayUbicacion) {
            persona = cuentaGateway.consultar(List.of(actual.getUsuarioCodigo())).stream().findFirst().orElse(null);
            if (persona == null || cambiaElPunto(persona, input.latitud(), input.longitud())) {
                persona = cuentaGateway.cambiarUbicacion(actual.getUsuarioCodigo(), input.latitud(), input.longitud());
                campos.add("ubicacion");
            }
        }

        // 3. La ficha
        Instructor guardada = instructorRepository.actualizar(cambios.build());
        if (persona == null) {
            persona = cuentaGateway.consultar(List.of(guardada.getUsuarioCodigo())).stream().findFirst().orElse(null);
        }

        LOG.info("Instructor actualizado", kv("module", MODULO), kv("instructorCodigo", guardada.getCodigo()),
            kv("escuelaCodigo", guardada.getEscuelaCodigo()), kv("campos", campos));
        return mapeador.aDetalle(guardada, persona);
    }

    /** Todas las siglas existen en el catálogo y ninguna es "sin categoría"; si no, `ERR_ESC_14`. */
    private Set<CategoriaLicencia> categoriasValidas(List<String> siglas) {
        List<CategoriaLicencia> encontradas = categoriaRepository.buscarPorSiglas(siglas);
        boolean hayInvalida = siglas.contains(Instructor.SIN_CATEGORIA) || encontradas.size() != siglas.size();
        if (hayInvalida) {
            LOG.warn("Edición rechazada: categoría inválida",
                kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.CATEGORIA_INVALIDA));
            throw new ValidationException(EscuelaErrorCodes.CATEGORIA_INVALIDA,
                "Alguna categoría no existe o no se puede enseñar");
        }
        return new LinkedHashSet<>(encontradas);
    }

    private boolean cambiaElPunto(PersonaInstructor persona, BigDecimal latitud, BigDecimal longitud) {
        return persona.latitud() == null || persona.longitud() == null
            || persona.latitud().compareTo(latitud) != 0 || persona.longitud().compareTo(longitud) != 0;
    }

    private ValidationException rechazo(String mensaje) {
        return new ValidationException(EscuelaErrorCodes.VALIDACION, mensaje);
    }
}
