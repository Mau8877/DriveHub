package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaComando;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaUseCase;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU05: el Super admin crea la cuenta del Administrador de una escuela.
 *
 * * El rol es SIEMPRE ADMIN_ESCUELA: no viene en el comando.
 * * No consulta a Escuela: en este momento la escuela todavía no existe (Escuela confirma
 * * su transacción después de que este servicio responde). Mientras no exista, la cuenta
 * * no puede iniciar sesión porque el login verifica la escuela.
 * ! Nunca se loguea la contraseña ni su hash.
 */
@Service
public class CrearAdministradorEscuelaService implements CrearAdministradorEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearAdministradorEscuelaService.class);
    private static final String MODULO = "usuarios";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;
    private final Clock reloj;
    private final int edadMinima;

    public CrearAdministradorEscuelaService(
            UsuarioRepositoryPort usuarioRepository,
            RolRepositoryPort rolRepository,
            PasswordHasherPort passwordHasher,
            Clock reloj,
            @Value("${app.registro.edad-minima:18}") int edadMinima) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordHasher = passwordHasher;
        this.reloj = reloj;
        this.edadMinima = edadMinima;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Usuario crear(CrearAdministradorEscuelaComando comando) {
        String email = comando.email().trim().toLowerCase(Locale.ROOT);
        if (edadEnAnios(comando.fechaNacimiento()) < edadMinima) {
            LOG.warn("Administrador rechazado: menor de edad",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.MENOR_DE_EDAD));
            throw new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD,
                "El administrador debe tener al menos " + edadMinima + " años");
        }
        if (usuarioRepository.existePorEmail(email)) {
            LOG.warn("Administrador rechazado: correo en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CORREO_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }
        if (usuarioRepository.existeAdministradorDeEscuela(comando.escuelaCodigo())) {
            LOG.warn("Administrador rechazado: la escuela ya tiene uno",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ADMIN_YA_EXISTE_EN_ESCUELA),
                kv("escuelaCodigo", comando.escuelaCodigo()));
            throw new ConflictException(IdentidadErrorCodes.ADMIN_YA_EXISTE_EN_ESCUELA,
                "La escuela ya tiene un administrador");
        }
        Rol rolAdmin = rolRepository.buscarPorNombre(RolNombres.ADMIN_ESCUELA)
            .orElseThrow(() -> new IllegalStateException("El rol ADMIN_ESCUELA no está sembrado en la base"));

        Usuario nuevo = Usuario.builder()
            .email(email)
            .passwordHash(passwordHasher.hashear(comando.password()))
            .nombres(comando.nombres().trim())
            .apellidos(comando.apellidos().trim())
            .telefono(comando.telefono() == null || comando.telefono().isBlank() ? null : comando.telefono().trim())
            .fechaNacimiento(comando.fechaNacimiento())
            .escuelaCodigo(comando.escuelaCodigo())
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .correoVerificado(false)
            .roles(Set.of(rolAdmin))
            .build();
        Usuario guardado = usuarioRepository.guardar(nuevo);
        LOG.info("Administrador de escuela creado", kv("module", MODULO),
            kv("usuarioCodigo", guardado.getCodigo()), kv("escuelaCodigo", comando.escuelaCodigo()));
        return guardado;
    }

    private int edadEnAnios(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears();
    }
}
