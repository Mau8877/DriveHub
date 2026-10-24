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
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La regla que comparten el registro del estudiante (CU01), el alta de estudiantes (CU08) y el alta
 * de instructores (CU09): edad mínima, correo y CI únicos, hash de la contraseña, dirección calculada
 * desde el punto y la creación de la cuenta (ACTIVA, correo sin verificar) junto con su perfil.
 *
 * * Quien la usa decide la escuela, el rol y la categoría: el registro saca la escuela del slug y
 * * arranca en "sin categoría"; el Administrador usa la suya; el alta de instructores usa
 * * INSTRUCTOR sin categoría.
 * * El código de la persona es opcional: si Escuela lo manda, la cuenta nace con ese código y
 * * Escuela puede revertirla aunque no reciba la respuesta (CU09).
 * * La escuela llega como `Supplier` para conservar el orden de las validaciones: la edad se
 * * comprueba antes de preguntarle a Escuela, y el correo y el CI después.
 * ! Nunca se loguea la contraseña ni su hash.
 */
@Component
public class AltaCuenta {

    private static final Logger LOG = LoggerFactory.getLogger(AltaCuenta.class);
    private static final String MODULO = "usuarios";

    /**
     * Los datos de la persona que se da de alta. El rol, la escuela y la categoría no vienen acá.
     * `codigo` es opcional: nulo = lo genera el servicio.
     */
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
            BigDecimal longitud,
            UUID codigo) {

        /** Sin código: lo genera el servicio al guardar. */
        public Datos(String nombres, String apellidos, LocalDate fechaNacimiento, String email, String password,
                String telefono, String ci, SexoPersona sexo, BigDecimal latitud, BigDecimal longitud) {
            this(nombres, apellidos, fechaNacimiento, email, password, telefono, ci, sexo, latitud, longitud, null);
        }
    }

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;
    private final GeocodificadorInversoPort geocodificador;
    private final Clock reloj;
    private final int edadMinima;

    public AltaCuenta(
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
     * @param rol nombre del rol de la cuenta (debe estar sembrado)
     * @throws ValidationException `ERR_IDE_02` si es menor de edad, `ERR_SYS_01` si el código dado ya existe
     * @throws ConflictException `ERR_IDE_01` si el correo ya está en uso, `ERR_IDE_19` si el CI lo está
     */
    public Usuario crear(Datos datos, Supplier<UUID> escuela, String rol, String categoriaSigla) {
        String email = datos.email().trim().toLowerCase(Locale.ROOT);

        if (edadEnAnios(datos.fechaNacimiento()) < edadMinima) {
            LOG.warn("Alta rechazada: menor de edad",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.MENOR_DE_EDAD));
            throw new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD,
                "Debes tener al menos " + edadMinima + " años para registrarte");
        }

        UUID escuelaCodigo = escuela.get();

        if (usuarioRepository.existePorEmail(email)) {
            LOG.warn("Alta rechazada: correo en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CORREO_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        String ci = Ci.normalizar(datos.ci());
        if (usuarioRepository.existePorCi(ci)) {
            LOG.warn("Alta rechazada: CI en uso",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CI_EN_USO));
            throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
        }

        if (datos.codigo() != null && usuarioRepository.buscarPorCodigo(datos.codigo()).isPresent()) {
            LOG.warn("Alta rechazada: el código ya existe", kv("module", MODULO));
            throw new ValidationException("ERR_SYS_01", "El código ya existe");
        }

        Rol rolCuenta = rolRepository.buscarPorNombre(rol)
            .orElseThrow(() -> new IllegalStateException("El rol " + rol + " no está sembrado en la base"));

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
            .codigo(datos.codigo())
            .email(email)
            .passwordHash(passwordHasher.hashear(datos.password()))
            .perfil(perfil)
            .escuelaCodigo(escuelaCodigo)
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .roles(Set.of(rolCuenta))
            .build();

        return usuarioRepository.guardar(nuevo);
    }

    private int edadEnAnios(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears();
    }
}
