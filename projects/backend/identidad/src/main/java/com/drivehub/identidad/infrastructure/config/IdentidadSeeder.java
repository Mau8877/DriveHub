package com.drivehub.identidad.infrastructure.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

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
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
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
 * * El Super admin no tiene escuela. adminexample e instructorexample pertenecen a la escuela
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

    private static final Logger LOG = LoggerFactory.getLogger(IdentidadSeeder.class);
    private static final String PASSWORD_DEV = "123123123";

    private final UsuarioRepositoryPort usuarioRepository;
    private final RolRepositoryPort rolRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearSiNoExiste(EMAIL_SUPER_ADMIN, "Super", "Admin", RolNombres.SUPER_ADMIN, null);
        crearSiNoExiste(EMAIL_ADMIN_EXAMPLE, "Admin", "Example", RolNombres.ADMIN_ESCUELA, CODIGO_ESCUELA_EXAMPLE);
        crearSiNoExiste(EMAIL_INSTRUCTOR_EXAMPLE, "Instructor", "Example", RolNombres.INSTRUCTOR, CODIGO_ESCUELA_EXAMPLE);
    }

    private void crearSiNoExiste(String email, String nombres, String apellidos, String rolNombre, UUID escuelaCodigo) {
        if (usuarioRepository.existePorEmail(email)) {
            return;
        }
        Rol rol = rolRepository.buscarPorNombre(rolNombre)
            .orElseThrow(() -> new IllegalStateException("El rol " + rolNombre + " no está sembrado en la base"));

        usuarioRepository.guardar(Usuario.builder()
            .email(email)
            .passwordHash(passwordHasher.hashear(PASSWORD_DEV))
            .nombres(nombres)
            .apellidos(apellidos)
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .escuelaCodigo(escuelaCodigo)
            .estado(EstadoUsuario.ACTIVO)
            .intentosFallidos(0)
            .roles(Set.of(rol))
            .build());
        LOG.info("Usuario de ejemplo creado para desarrollo", kv("module", "usuarios"), kv("rol", rolNombre));
    }
}
