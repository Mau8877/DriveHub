package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorComando;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorUseCase;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela da de alta la cuenta y el perfil de un instructor en SU escuela.
 *
 * * La escuela sale de la cuenta del Administrador, nunca del comando. La cuenta queda ACTIVA, con
 * * el correo sin verificar, sin categoría y con el código que eligió Escuela.
 * * Las reglas de datos (edad, correo y CI únicos, perfil) son las de {@link AltaCuenta}.
 * * La ficha profesional (licencia, categorías, estado) la guarda Escuela, no este servicio.
 * ! Nunca se loguea la contraseña, el CI, el correo, el teléfono ni las coordenadas.
 */
@Service
@RequiredArgsConstructor
public class CrearCuentaInstructorService implements CrearCuentaInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearCuentaInstructorService.class);
    private static final String MODULO = "instructores";

    private final EscuelaDelAdministrador escuelaDelAdministrador;
    private final AltaCuenta altaCuenta;
    private final PersonaInstructorMapeador mapeador;

    @Override
    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public PersonaInstructor crear(CrearCuentaInstructorComando comando) {
        UUID escuela = escuelaDelAdministrador.de(comando.administradorCodigo());

        AltaCuenta.Datos datos = new AltaCuenta.Datos(comando.nombres(), comando.apellidos(),
            comando.fechaNacimiento(), comando.email(), comando.password(), comando.telefono(), comando.ci(),
            comando.sexo(), comando.latitud(), comando.longitud(), comando.codigo());
        Usuario guardado = altaCuenta.crear(datos, () -> escuela, RolNombres.INSTRUCTOR, null);

        LOG.info("Cuenta de instructor creada", kv("module", MODULO),
            kv("usuarioCodigo", guardado.getCodigo()), kv("administradorCodigo", comando.administradorCodigo()));
        return mapeador.aPersona(guardado);
    }
}
