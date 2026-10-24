package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarUbicacionInstructorUseCase;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela corrige la ubicación de un instructor de SU escuela.
 *
 * * Solo cambia `latitud`, `longitud` y `direccion` del perfil. Si el punto es el mismo, no se
 * * consulta el mapa; si el geocodificador no responde, la dirección queda en blanco (sin error).
 * ! En los registros nunca van las coordenadas.
 */
@Service
@RequiredArgsConstructor
public class EditarUbicacionInstructorService implements EditarUbicacionInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarUbicacionInstructorService.class);
    private static final String MODULO = "instructores";

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final GeocodificadorInversoPort geocodificador;
    private final PersonaInstructorMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PersonaInstructor editar(
            UUID administradorCodigo, UUID instructorCodigo, BigDecimal latitud, BigDecimal longitud) {
        UUID escuela = escuelaDelAdministrador.de(administradorCodigo);
        Usuario instructor = usuarioRepository.buscarInstructorDeEscuela(escuela, instructorCodigo)
            .orElseThrow(() -> {
                LOG.warn("Edición rechazada: instructor no encontrado en la escuela",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
                return new NotFoundException(IdentidadErrorCodes.INSTRUCTOR_NO_ENCONTRADO,
                    "El instructor no existe");
            });

        if (latitud == null || longitud == null) {
            throw new ValidationException("ERR_SYS_01", "La latitud y la longitud son obligatorias y van juntas");
        }

        Perfil actual = instructor.getPerfil();
        Perfil.PerfilBuilder cambios = actual.toBuilder().latitud(latitud).longitud(longitud);
        if (cambiaElPunto(actual, latitud, longitud)) {
            Optional<String> direccion = geocodificador.direccionDe(latitud, longitud);
            if (direccion.isEmpty()) {
                LOG.warn("El geocodificador no devolvió dirección: queda en blanco", kv("module", MODULO),
                    kv("usuarioCodigo", instructorCodigo));
            }
            cambios.direccion(direccion.orElse(null));
        }

        Usuario guardado = usuarioRepository.actualizar(instructor.toBuilder().perfil(cambios.build()).build());
        LOG.info("Ubicación de instructor actualizada", kv("module", MODULO),
            kv("usuarioCodigo", guardado.getCodigo()), kv("administradorCodigo", administradorCodigo));
        return mapeador.aPersona(guardado);
    }

    private boolean cambiaElPunto(Perfil actual, BigDecimal latitud, BigDecimal longitud) {
        return actual.getLatitud() == null || actual.getLongitud() == null
            || actual.getLatitud().compareTo(latitud) != 0 || actual.getLongitud().compareTo(longitud) != 0;
    }
}
