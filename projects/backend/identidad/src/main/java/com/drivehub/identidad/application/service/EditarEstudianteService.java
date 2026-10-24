package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarEstudianteComando;
import com.drivehub.identidad.domain.port.in.EditarEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * CU08 (F3): el Administrador de escuela corrige la categoría y/o la ubicación de un estudiante de
 * SU escuela. Nada más: nombres, CI, sexo, fecha de nacimiento, teléfono, correo, estado y escuela
 * no se tocan nunca desde acá.
 *
 * * Un estudiante inexistente y uno de otra escuela responden igual (ERR_IDE_20).
 * * La categoría se valida con Escuela (que tiene el catálogo), salvo "sin categoría".
 * * Si el punto cambió se recalcula la dirección; si no se puede calcular queda en blanco.
 * ! En los registros solo van los NOMBRES de los campos modificados, nunca sus valores.
 */
@Service
@RequiredArgsConstructor
public class EditarEstudianteService implements EditarEstudianteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarEstudianteService.class);
    private static final String MODULO = "estudiantes";
    private static final String VALIDACION = "ERR_SYS_01";

    private final UsuarioRepositoryPort usuarioRepository;
    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final CategoriaLicenciaConsultaPort categoriaConsulta;
    private final GeocodificadorInversoPort geocodificador;
    private final EstudianteMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public EstudianteDetalle editar(EditarEstudianteComando comando) {
        UUID escuela = escuelaDelAdministrador.de(comando.administradorCodigo());
        Usuario estudiante = usuarioRepository.buscarEstudianteDeEscuela(escuela, comando.estudianteCodigo())
            .orElseThrow(() -> {
                LOG.warn("Edición rechazada: estudiante no encontrado en la escuela",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO));
                return new NotFoundException(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO,
                    "El estudiante no existe");
            });

        String categoria = comando.categoriaSigla() == null || comando.categoriaSigla().isBlank()
            ? null : comando.categoriaSigla().trim().toUpperCase(Locale.ROOT);
        boolean hayUbicacion = comando.latitud() != null || comando.longitud() != null;
        if (categoria == null && !hayUbicacion) {
            throw new ValidationException(VALIDACION, "No hay nada que cambiar");
        }
        if (hayUbicacion && (comando.latitud() == null || comando.longitud() == null)) {
            throw new ValidationException(VALIDACION, "La latitud y la longitud van juntas");
        }

        Perfil.PerfilBuilder cambios = estudiante.getPerfil().toBuilder();
        List<String> campos = new ArrayList<>();

        if (categoria != null) {
            comprobarCategoria(categoria);
            cambios.categoriaSigla(categoria);
            campos.add("categoria");
        }
        if (hayUbicacion && cambiaElPunto(estudiante.getPerfil(), comando.latitud(), comando.longitud())) {
            cambios.latitud(comando.latitud()).longitud(comando.longitud())
                .direccion(geocodificador.direccionDe(comando.latitud(), comando.longitud()).orElse(null));
            campos.add("ubicacion");
        }

        Usuario guardado = usuarioRepository.actualizar(estudiante.toBuilder().perfil(cambios.build()).build());
        LOG.info("Estudiante actualizado por el administrador", kv("module", MODULO),
            kv("usuarioCodigo", guardado.getCodigo()), kv("administradorCodigo", comando.administradorCodigo()),
            kv("campos", campos));
        return mapeador.aDetalle(guardado);
    }

    private void comprobarCategoria(String categoria) {
        if (!CategoriaNombres.SIN_CATEGORIA.equals(categoria) && !categoriaConsulta.existe(categoria)) {
            LOG.warn("Edición rechazada: la categoría no existe",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CATEGORIA_NO_EXISTE));
            throw new ValidationException(IdentidadErrorCodes.CATEGORIA_NO_EXISTE,
                "La categoría de licencia no existe");
        }
    }

    private boolean cambiaElPunto(Perfil actual, BigDecimal latitud, BigDecimal longitud) {
        return actual.getLatitud() == null || actual.getLongitud() == null
            || actual.getLatitud().compareTo(latitud) != 0 || actual.getLongitud().compareTo(longitud) != 0;
    }
}
