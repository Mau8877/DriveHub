package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearEstudianteComando;
import com.drivehub.identidad.domain.port.in.CrearEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;

import lombok.RequiredArgsConstructor;

/**
 * CU08 (F2): el Administrador de escuela da de alta a un estudiante en SU escuela.
 *
 * * La escuela sale de la cuenta del Administrador, nunca del comando. La cuenta queda ACTIVA, con
 * * el correo sin verificar y la contraseña inicial que escribió el Administrador.
 * * La categoría es opcional ("sin categoría" por defecto); otra se valida con Escuela, que tiene el
 * * catálogo. Si Escuela no responde, el alta falla y no se crea nada.
 * * Las reglas de datos (edad, correo y CI únicos, perfil) son las de {@link AltaCuenta}.
 * ! Nunca se loguea la contraseña, el CI, el correo, el teléfono ni las coordenadas.
 */
@Service
@RequiredArgsConstructor
public class CrearEstudianteService implements CrearEstudianteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearEstudianteService.class);
    private static final String MODULO = "estudiantes";

    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final CategoriaLicenciaConsultaPort categoriaConsulta;
    private final AltaCuenta altaCuenta;
    private final EstudianteMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public EstudianteDetalle crear(CrearEstudianteComando comando) {
        UUID escuela = escuelaDelAdministrador.de(comando.administradorCodigo());
        String categoria = categoriaElegida(comando.categoriaSigla());

        AltaCuenta.Datos datos = new AltaCuenta.Datos(comando.nombres(), comando.apellidos(),
            comando.fechaNacimiento(), comando.email(), comando.password(), comando.telefono(), comando.ci(),
            comando.sexo(), comando.latitud(), comando.longitud());
        Usuario guardado = altaCuenta.crear(datos, () -> escuela, RolNombres.ESTUDIANTE, categoria);

        LOG.info("Estudiante creado por el administrador", kv("module", MODULO),
            kv("usuarioCodigo", guardado.getCodigo()), kv("administradorCodigo", comando.administradorCodigo()));
        return mapeador.aDetalle(guardado);
    }

    /** Nula o en blanco es "sin categoría"; otra se pasa a mayúsculas y se valida contra el catálogo. */
    private String categoriaElegida(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return CategoriaNombres.SIN_CATEGORIA;
        }
        String categoria = sigla.trim().toUpperCase(Locale.ROOT);
        if (!CategoriaNombres.SIN_CATEGORIA.equals(categoria) && !categoriaConsulta.existe(categoria)) {
            LOG.warn("Alta rechazada: la categoría no existe",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CATEGORIA_NO_EXISTE));
            throw new ValidationException(IdentidadErrorCodes.CATEGORIA_NO_EXISTE,
                "La categoría de licencia no existe");
        }
        return categoria;
    }
}
