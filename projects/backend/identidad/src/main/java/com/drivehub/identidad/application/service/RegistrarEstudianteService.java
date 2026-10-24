package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteUseCase;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;

import lombok.RequiredArgsConstructor;

/**
 * CU01 + CU08a: un estudiante crea su propia cuenta y su perfil.
 *
 * * El rol es SIEMPRE ESTUDIANTE: no viene en el comando.
 * * La cuenta queda ACTIVA y sin sesión (no hay auto-login, ese es CU02).
 * * La categoría arranca en "sin categoría": solo la escuela la cambia (CU08).
 * * La dirección la calcula el servidor desde la ubicación; si no puede, se guarda en blanco.
 * * Las reglas de datos (edad, correo y CI únicos, perfil) son las de {@link AltaEstudiante}, la
 * * misma que usa el Administrador al dar de alta a un estudiante (CU08).
 * ! Nunca se loguea la contraseña ni su hash.
 */
@Service
@RequiredArgsConstructor
public class RegistrarEstudianteService implements RegistrarEstudianteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarEstudianteService.class);
    private static final String MODULO = "usuarios";

    private final EscuelaConsultaPort escuelaConsulta;
    private final AltaEstudiante altaEstudiante;

    @Override
    @Transactional
    public Usuario registrar(RegistrarEstudianteComando comando) {
        AltaEstudiante.Datos datos = new AltaEstudiante.Datos(comando.nombres(), comando.apellidos(),
            comando.fechaNacimiento(), comando.email(), comando.password(), comando.telefono(), comando.ci(),
            comando.sexo(), comando.latitud(), comando.longitud());

        Usuario guardado = altaEstudiante.crear(datos, () -> escuelaDelSlug(comando.escuelaSlug()),
            CategoriaNombres.SIN_CATEGORIA);

        LOG.info("Estudiante registrado", kv("module", MODULO), kv("usuarioCodigo", guardado.getCodigo()));
        return guardado;
    }

    private UUID escuelaDelSlug(String slug) {
        return escuelaConsulta.buscarActivaPorSlug(slug.trim().toLowerCase(Locale.ROOT))
            .map(escuela -> escuela.codigo())
            .orElseThrow(() -> {
                LOG.warn("Registro rechazado: escuela no disponible",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE));
                return new NotFoundException(IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE,
                    "La escuela no existe o no está disponible");
            });
    }
}
