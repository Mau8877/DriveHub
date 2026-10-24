package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Ci;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La regla que comparten el registro del estudiante (CU01) y el alta por el Administrador (CU08):
 * edad mínima, correo y CI únicos, hash de la contraseña, dirección calculada desde el punto y la
 * creación de la cuenta (rol ESTUDIANTE, ACTIVA, correo sin verificar) junto con su perfil.
 *
 * * Quien la usa decide la escuela y la categoría: el registro la saca del slug y arranca en "sin
 * * categoría"; el Administrador usa la suya y elige la categoría.
 * * La escuela llega como `Supplier` para conservar el orden de las validaciones: la edad se
 * * comprueba antes de preguntarle a Escuela, y el correo y el CI después.
 * ! Nunca se loguea la contraseña ni su hash.
 */
@Component
public class AltaEstudiante {

    private static final Logger LOG = LoggerFactory.getLogger(AltaEstudiante.class);
    private static final String MODULO = "usuarios";

    /** Los datos de la persona que se da de alta. El rol, la escuela y la categoría no vienen acá. */
    public record Datos(
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String email,
            String password,
            String telefono,
            String ci,
            SexoPersona sexo,
            BigDecimal latitud,
            BigDecimal longitud) {
    }

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;
    private final GeocodificadorInversoPort geocodificador;
    private final Clock reloj;
    private final int edadMinima;

    public AltaEstudiante(
            UsuarioRepositoryPort usuarioRepository,
            RolRepositoryPort rolRepository,
            PasswordHasherPort passwordHasher,
            GeocodificadorInversoPort geocodificador,
            Clock reloj,
            @Value("${app.registro.edad-minima:18}") int edadMinima) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordHasher = passwordHasher;
        this.geocodificador = geocodificador;
        this.reloj = reloj;
        this.edadMinima = edadMinima;
    }

    /**
     * Crea la cuenta y su perfil. Debe correr dentro de la transacción de quien la llama.
     *
     * @param escuela quién es la escuela de la cuenta; se pide después de validar la edad
     * @throws ValidationException `ERR_IDE_02` si es menor de edad
     * @throws ConflictException `ERR_IDE_01` si el correo ya está en uso, `ERR_IDE_19` si el CI lo está
     */
    public Usuario crear(Datos datos, Supplier<UUID> escuela, String categoriaSigla) {
        String email = datos.email().trim().toLowerCase(Locale.ROOT);

        if (edadEnAnios(datos.fechaNacimiento()) < edadMinima) {
            LOG.warn("Registro rechazado: menor de edad",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.MENOR_DE_EDAD));
            throw new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD,
                "Debes tener al menos " + edadMinima + " años para registrarte");
        }

        UUID escuelaCodigo = escuela.get();

        if (usuarioRepository.existePorEmail(email)) {
            LOG.warn("Registro rechazado: correo en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CORREO_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        String ci = Ci.normalizar(datos.ci());
        if (usuarioRepository.existePorCi(ci)) {
            LOG.warn("Registro rechazado: CI en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CI_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
        }

        Rol rolEstudiante = rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE)
            .orElseThrow(() -> new IllegalStateException("El rol ESTUDIANTE no está sembrado en la base"));

        Perfil perfil = Perfil.builder()
            .nombres(datos.nombres().trim())
            .apellidos(datos.apellidos().trim())
            .telefono(datos.telefono() == null || datos.telefono().isBlank() ? null : datos.telefono().trim())
            .fechaNacimiento(datos.fechaNacimiento())
            .ci(ci)
            .sexo(datos.sexo())
            .direccion(geocodificador.direccionDe(datos.latitud(), datos.longitud()).orElse(null))
            .latitud(datos.latitud())
            .longitud(datos.longitud())
            .categoriaSigla(categoriaSigla)
            .build();

        Usuario nuevo = Usuario.builder()
            .email(email)
            .passwordHash(passwordHasher.hashear(datos.password()))
            .perfil(perfil)
            .escuelaCodigo(escuelaCodigo)
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .roles(Set.of(rolEstudiante))
            .build();

        return usuarioRepository.guardar(nuevo);
    }

    private int edadEnAnios(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears();
    }
}
