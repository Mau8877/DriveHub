package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU09 (parte de Identidad) de punta a punta: controller, servicios con su @PreAuthorize, adaptadores
 * y Postgres real (con `unaccent`), con Escuela y el mapa simulados. Aquí se ejerce el ROL y el
 * aislamiento por escuela, que los tests unitarios no pueden cubrir.
 *
 * * Cada prueba usa sus propias escuelas (UUID nuevos): la base se comparte entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InstructoresCuentaIntegrationTest extends PostgresIntegrationTestBase {

    private static final String RUTA = "/api/v1/usuarios/instructores";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioRepositoryPort usuarios;
    @Autowired
    private RolRepositoryPort roles;

    @MockitoBean
    private GeocodificadorInversoPort geocodificador;
    @MockitoBean
    private EscuelaConsultaPort escuelas;

    private Usuario cuenta(UUID escuela, String rol, String nombres, String apellidos) {
        Rol elRol = roles.buscarPorNombre(rol).orElseThrow();
        return usuarios.guardar(Usuario.builder()
            .email(UUID.randomUUID() + "@gmail.com").passwordHash("$2a$hash")
            .ci(CiDePrueba.unico()).sexo(SexoPersona.MASCULINO).nombres(nombres).apellidos(apellidos)
            .fechaNacimiento(LocalDate.of(1990, 1, 1)).escuelaCodigo(escuela)
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(Set.of(elRol)).build());
    }

    private static RequestPostProcessor como(Usuario usuario, String rol) {
        return jwt().jwt(j -> j.subject(usuario.getCodigo().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static String cuerpoCrear(UUID codigo, String email, String ci) {
        return """
            {"codigo":"%s","nombres":"Ivan","apellidos":"Pérez","fechaNacimiento":"1990-01-01","sexo":"MASCULINO",
             "ci":"%s","email":"%s","password":"123456789","latitud":-17.7833,"longitud":-63.1821}
            """.formatted(codigo, ci, email);
    }

    private ResultActions crear(RequestPostProcessor sesion, UUID codigo, String email, String ci) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(codigo, email, ci)));
    }

    private int cuentasConCorreo(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class, email);
    }

    @Test
    void ca14_cualquierRolQueNoSeaAdministradorDeEscuelaRecibe403EnLosCincoEndpoints() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario instructor = cuenta(escuela, RolNombres.INSTRUCTOR, "Ivan", "Alfa");
        String correo = "ca14-" + UUID.randomUUID() + "@gmail.com";

        for (String rol : new String[] {"ESTUDIANTE", "INSTRUCTOR", "SUPER_ADMIN"}) {
            RequestPostProcessor sesion = como(instructor, rol);
            crear(sesion, UUID.randomUUID(), correo, CiDePrueba.unico()).andExpect(status().isForbidden());
            mockMvc.perform(post(RUTA + "/consulta").with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigos\":[\"" + instructor.getCodigo() + "\"]}")).andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA).with(sesion)).andExpect(status().isForbidden());
            mockMvc.perform(patch(RUTA + "/" + instructor.getCodigo() + "/ubicacion").with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
                .andExpect(status().isForbidden());
            mockMvc.perform(delete(RUTA + "/" + instructor.getCodigo()).with(sesion)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        assertThat(cuentasConCorreo(correo)).isZero();
        assertThat(usuarios.buscarPorCodigo(instructor.getCodigo())).isPresent();
    }

    @Test
    void ca07_elAdministradorCreaLaCuentaEnSuEscuelaConElCodigoPedidoYLuegoInstructorIniciaSesionDesdeElMovil()
            throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Cañoto 100"));
        when(escuelas.buscarPorCodigo(escuela)).thenReturn(Optional.of(new EscuelaRef(escuela, "esc", "Esc", true)));
        UUID codigo = UUID.randomUUID();
        String correo = "ca07-" + UUID.randomUUID() + "@gmail.com";

        crear(como(admin, "ADMIN_ESCUELA"), codigo, correo, CiDePrueba.unico())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.direccion").value("Av. Cañoto 100"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT escuela_codigo::text FROM usuario WHERE email = ?", String.class, correo))
            .isEqualTo(escuela.toString());
        assertThat(jdbc.queryForObject("SELECT correo_verificado FROM usuario WHERE email = ?", Boolean.class, correo))
            .isFalse();
        assertThat(jdbc.queryForObject("SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
            + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = ?", String.class, correo)).isEqualTo("INSTRUCTOR");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + correo + "\",\"password\":\"123456789\",\"dispositivo\":\"MOVIL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void ca08_correoOCiRepetidosYMenoresDeEdadNoCreanNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario existente = cuenta(escuela, RolNombres.INSTRUCTOR, "Ivan", "Alfa");
        RequestPostProcessor sesion = como(admin, "ADMIN_ESCUELA");
        String correo = "ca08-" + UUID.randomUUID() + "@gmail.com";

        crear(sesion, UUID.randomUUID(), existente.getEmail(), CiDePrueba.unico())
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(sesion, UUID.randomUUID(), correo, existente.getPerfil().getCi())
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
        mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(UUID.randomUUID(), correo, CiDePrueba.unico())
                .replace("1990-01-01", LocalDate.now().minusYears(10).toString())))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));

        assertThat(cuentasConCorreo(correo)).isZero();
    }

    @Test
    void ca05_laConsultaPorLoteYLaBusquedaSoloVenInstructoresDeLaEscuelaDelAdministrador() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario adminA = cuenta(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario deA = cuenta(escuelaA, RolNombres.INSTRUCTOR, "José", "Gómez");
        Usuario deB = cuenta(escuelaB, RolNombres.INSTRUCTOR, "José", "Beta");
        Usuario estudiante = cuenta(escuelaA, RolNombres.ESTUDIANTE, "José", "Estudiante");
        RequestPostProcessor sesion = como(adminA, "ADMIN_ESCUELA");

        mockMvc.perform(post(RUTA + "/consulta").with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigos\":[\"" + deA.getCodigo() + "\",\"" + deB.getCodigo() + "\",\""
                    + estudiante.getCodigo() + "\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].codigo").value(deA.getCodigo().toString()));

        mockMvc.perform(get(RUTA).param("search", "jose gomez").with(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("José Gómez"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(RUTA).param("search", "estudiante").with(sesion))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void laUbicacionSeEditaEnLosDeSuEscuelaYUnoDeOtraRecibeErrIde22() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario adminA = cuenta(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario deA = cuenta(escuelaA, RolNombres.INSTRUCTOR, "Ivan", "Alfa");
        Usuario deB = cuenta(escuelaB, RolNombres.INSTRUCTOR, "Ines", "Beta");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Nueva 10"));
        RequestPostProcessor sesion = como(adminA, "ADMIN_ESCUELA");

        mockMvc.perform(patch(RUTA + "/" + deA.getCodigo() + "/ubicacion").with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.direccion").value("Av. Nueva 10"));
        mockMvc.perform(patch(RUTA + "/" + deB.getCodigo() + "/ubicacion").with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_22"));
    }

    @Test
    void ca12_revertirBorraUnaCuentaSinSesionSeRepiteSinErrorYUnaYaUsadaDaErrIde23() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario sinUsar = cuenta(escuela, RolNombres.INSTRUCTOR, "Ivan", "Alfa");
        Usuario usado = cuenta(escuela, RolNombres.INSTRUCTOR, "Ines", "Beta");
        Usuario estudiante = cuenta(escuela, RolNombres.ESTUDIANTE, "Eva", "Gamma");
        jdbc.update("UPDATE usuario SET ultimo_login = now() WHERE codigo_usuario = ?", usado.getCodigo());
        RequestPostProcessor sesion = como(admin, "ADMIN_ESCUELA");

        mockMvc.perform(delete(RUTA + "/" + sinUsar.getCodigo()).with(sesion)).andExpect(status().isOk());
        assertThat(cuentasConCorreo(sinUsar.getEmail())).isZero();
        mockMvc.perform(delete(RUTA + "/" + sinUsar.getCodigo()).with(sesion)).andExpect(status().isOk());

        mockMvc.perform(delete(RUTA + "/" + usado.getCodigo()).with(sesion))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_23"));
        assertThat(cuentasConCorreo(usado.getEmail())).isEqualTo(1);

        // * Un estudiante no se puede revertir por esta vía: para este endpoint no existe
        mockMvc.perform(delete(RUTA + "/" + estudiante.getCodigo()).with(sesion)).andExpect(status().isOk());
        assertThat(cuentasConCorreo(estudiante.getEmail())).isEqualTo(1);
    }
}
