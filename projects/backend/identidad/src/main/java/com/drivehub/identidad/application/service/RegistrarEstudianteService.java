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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.Ci;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteUseCase;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU01 + CU08a: un estudiante crea su propia cuenta y su perfil.
 *
 * * El rol es SIEMPRE ESTUDIANTE: no viene en el comando.
 * * La cuenta queda ACTIVA y sin sesión (no hay auto-login, ese es CU02).
 * * La categoría arranca en "sin categoría": solo la escuela la cambia (CU08).
 * * La dirección la calcula el servidor desde la ubicación; si no puede, se guarda en blanco.
 * ! Nunca se loguea la contraseña ni su hash.
 */
@Service
public class RegistrarEstudianteService implements RegistrarEstudianteUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarEstudianteService.class);
    private static final String MODULO = "usuarios";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final EscuelaConsultaPort escuelaConsulta;
    private final PasswordHasherPort passwordHasher;
    private final GeocodificadorInversoPort geocodificador;
    private final Clock reloj;
    private final int edadMinima;

    public RegistrarEstudianteService(
            UsuarioRepositoryPort usuarioRepository,
            RolRepositoryPort rolRepository,
            EscuelaConsultaPort escuelaConsulta,
            PasswordHasherPort passwordHasher,
            GeocodificadorInversoPort geocodificador,
            Clock reloj,
            @Value("${app.registro.edad-minima:18}") int edadMinima) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.escuelaConsulta = escuelaConsulta;
        this.passwordHasher = passwordHasher;
        this.geocodificador = geocodificador;
        this.reloj = reloj;
        this.edadMinima = edadMinima;
    }

    @Override
    @Transactional
    public Usuario registrar(RegistrarEstudianteComando comando) {
        String email = comando.email().trim().toLowerCase(Locale.ROOT);

        if (edadEnAnios(comando.fechaNacimiento()) < edadMinima) {
            LOG.warn("Registro rechazado: menor de edad",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.MENOR_DE_EDAD));
            throw new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD,
                "Debes tener al menos " + edadMinima + " años para registrarte");
        }

        EscuelaRef escuela = escuelaConsulta.buscarActivaPorSlug(comando.escuelaSlug().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> {
                LOG.warn("Registro rechazado: escuela no disponible",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE));
                return new NotFoundException(IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE,
                    "La escuela no existe o no está disponible");
            });

        if (usuarioRepository.existePorEmail(email)) {
            LOG.warn("Registro rechazado: correo en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CORREO_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        String ci = Ci.normalizar(comando.ci());
        if (usuarioRepository.existePorCi(ci)) {
            LOG.warn("Registro rechazado: CI en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CI_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
        }

        Rol rolEstudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE)
            .orElseThrow(() -> new IllegalStateException("El rol ESTUDIANTE no está sembrado en la base"));

        Perfil perfil = Perfil.builder()
            .nombres(comando.nombres().trim())
            .apellidos(comando.apellidos().trim())
            .telefono(comando.telefono() == null || comando.telefono().isBlank() ? null : comando.telefono().trim())
            .fechaNacimiento(comando.fechaNacimiento())
            .ci(ci)
            .sexo(comando.sexo())
            .direccion(geocodificador.direccionDe(comando.latitud(), comando.longitud()).orElse(null))
            .latitud(comando.latitud())
            .longitud(comando.longitud())
            .categoriaSigla(CategoriaNombres.SIN_CATEGORIA)
            .build();

        Usuario nuevo = Usuario.builder()
            .email(email)
            .passwordHash(passwordHasher.hashear(comando.password()))
            .perfil(perfil)
            .escuelaCodigo(escuela.codigo())
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .roles(Set.of(rolEstudiante))
            .build();

        Usuario guardado = usuarioRepository.guardar(nuevo);
        LOG.info("Estudiante registrado", kv("module", MODULO), kv("usuarioCodigo", guardado.getCodigo()));
        return guardado;
    }

    private int edadEnAnios(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears();
    }
}
