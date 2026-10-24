package com.drivehub.identidad.infrastructure.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Datos de ejemplo para desarrollo: el Super admin, el Administrador y un
 * Instructor de la escuela de ejemplo.
 *
 * ! Solo con el perfil `dev`, con una contraseña conocida (123123123):
 * ! NUNCA se siembra en producción.
 * * Cada cuenta lleva su perfil (CU08a) con un CI propio; el Administrador y el Instructor tienen una
 * * ubicación fija de La Paz, con la dirección escrita acá: el arranque no depende del servicio de mapas.
 * * El Super admin no tiene escuela ni ubicación. adminexample e instructorexample pertenecen a la escuela
 * * `escuelaexample`, cuyo codigo fijo es el mismo que siembra el servicio
 * * Escuela (EscuelaSeeder): así quedan enlazados sin llamarse entre sí.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class IdentidadSeeder implements ApplicationRunner {

    public static final UUID CODIGO_ESCUELA_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");
    public static final String EMAIL_SUPER_ADMIN = "superadmin@gmail.com";
    public static final String EMAIL_ADMIN_EXAMPLE = "adminexample@gmail.com";
    public static final String EMAIL_INSTRUCTOR_EXAMPLE = "instructorexample@gmail.com";
    public static final String CI_SUPER_ADMIN = "SA0000001";
    public static final String CI_ADMIN_EXAMPLE = "AD0000001";
    public static final String CI_INSTRUCTOR_EXAMPLE = "IN0000001";

    private static final BigDecimal LATITUD_LA_PAZ = new BigDecimal("-16.489700");
    private static final BigDecimal LONGITUD_LA_PAZ = new BigDecimal("-68.119300");
    private static final String DIRECCION_LA_PAZ = "Plaza Murillo, La Paz, Bolivia";

    private static final Logger LOG = LoggerFactory.getLogger(IdentidadSeeder.class);
    private static final String PASSWORD_DEV = "123123123";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearSiNoExiste(EMAIL_SUPER_ADMIN, "Super", "Admin", CI_SUPER_ADMIN, RolNombres.SUPER_ADMIN, null, false);
        crearSiNoExiste(EMAIL_ADMIN_EXAMPLE, "Admin", "Example", CI_ADMIN_EXAMPLE, RolNombres.ADMIN_ESCUELA,
            CODIGO_ESCUELA_EXAMPLE, true);
        crearSiNoExiste(EMAIL_INSTRUCTOR_EXAMPLE, "Instructor", "Example", CI_INSTRUCTOR_EXAMPLE,
            RolNombres.INSTRUCTOR, CODIGO_ESCUELA_EXAMPLE, true);
    }

    private void crearSiNoExiste(String email, String nombres, String apellidos, String ci, String rolNombre,
            UUID escuelaCodigo, boolean conUbicacion) {
        if (usuarioRepository.existePorEmail(email)) {
            return;
        }
        Rol rol = rolRepository.buscarPorNombre(rolNombre)
            .orElseThrow(() -> new IllegalStateException("El rol " + rolNombre + " no está sembrado en la base"));

        Perfil perfil = Perfil.builder()
            .nombres(nombres)
            .apellidos(apellidos)
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .ci(ci)
            .sexo(SexoPersona.MASCULINO)
            .direccion(conUbicacion ? DIRECCION_LA_PAZ : null)
            .latitud(conUbicacion ? LATITUD_LA_PAZ : null)
            .longitud(conUbicacion ? LONGITUD_LA_PAZ : null)
            .build();

        usuarioRepository.guardar(Usuario.builder()
            .email(email)
            .passwordHash(passwordHasher.hashear(PASSWORD_DEV))
            .perfil(perfil)
            .escuelaCodigo(escuelaCodigo)
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .roles(Set.of(rol))
            .build());
        LOG.info("Usuario de ejemplo creado para desarrollo", kv("module", "usuarios"), kv("rol", rolNombre));
    }
}
