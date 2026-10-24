package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU08 de punta a punta: controller, servicios con su @PreAuthorize, adaptadores y Postgres real
 * (con `unaccent`), con Escuela y el mapa simulados. Aquí se ejerce el ROL y el aislamiento por
 * escuela, que los tests unitarios no pueden cubrir. Cada método nombra el criterio (CAxx).
 *
 * * Cada prueba usa sus propias escuelas (UUID nuevos): la base se comparte entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EstudiantesIntegrationTest extends PostgresIntegrationTestBase {

    private static final String RUTA = "/api/v1/estudiantes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioRepositoryPort usuarios;
    @Autowired
    private RolRepositoryPort roles;

    @MockitoBean
    private CategoriaLicenciaConsultaPort categorias;
    @MockitoBean
    private GeocodificadorInversoPort geocodificador;
    @MockitoBean
    private EscuelaConsultaPort escuelas;

    private Usuario cuenta(UUID escuela, String rol, String nombres, String apellidos) {
        Rol elRol = roles.buscarPorNombre(rol).orElseThrow();
        return usuarios.guardar(Usuario.builder()
            .email(UUID.randomUUID() + "@gmail.com").passwordHash("$2a$hash")
            .ci(CiDePrueba.unico()).sexo(SexoPersona.MASCULINO).nombres(nombres).apellidos(apellidos)
            .fechaNacimiento(LocalDate.of(2000, 1, 1)).escuelaCodigo(escuela)
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(Set.of(elRol)).build());
    }

    private static RequestPostProcessor como(Usuario usuario, String rol) {
        return jwt().jwt(j -> j.subject(usuario.getCodigo().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static String cuerpoCrear(String email, String ci, String extra) {
        return """
            {"nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"2000-01-01","sexo":"FEMENINO","ci":"%s",
             "email":"%s","password":"123456789","latitud":-17.7833,"longitud":-63.1821%s}
            """.formatted(ci, email, extra);
    }

    private ResultActions crear(RequestPostProcessor sesion, String email, String ci, String extra) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(email, ci, extra)));
    }

    private int cuentasConCorreo(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class, email);
    }

    @Test
    void ca11_cualquierRolQueNoSeaAdministradorDeEscuelaRecibe403YNoSeCreaNiCambiaNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario estudiante = cuenta(escuela, RolNombres.ESTUDIANTE, "Ana", "Alfa");
        String correo = "ca11-" + UUID.randomUUID() + "@gmail.com";

        for (String rol : new String[] {"ESTUDIANTE", "INSTRUCTOR", "SUPER_ADMIN"}) {
            RequestPostProcessor sesion = como(estudiante, rol);
            mockMvc.perform(get(RUTA).with(sesion)).andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA + "/" + estudiante.getCodigo()).with(sesion)).andExpect(status().isForbidden());
            crear(sesion, correo, CiDePrueba.unico(), "").andExpect(status().isForbidden());
            mockMvc.perform(patch(RUTA + "/" + estudiante.getCodigo()).with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"categoriaSigla\":\"B\"}"))
                .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        assertThat(cuentasConCorreo(correo)).isZero();
        assertThat(jdbc.queryForObject("SELECT p.categoria_sigla FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.codigo_usuario = ?", String.class, estudiante.getCodigo())).isNull();
        verify(categorias, never()).existe(any());
    }

    @Test
    void ca01_ca05_elAdministradorListaSoloLosEstudiantesDeSuEscuelaAunqueOtraTengaMas() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario adminA = cuenta(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        cuenta(escuelaA, RolNombres.ESTUDIANTE, "Ana", "Alfa");
        cuenta(escuelaA, RolNombres.INSTRUCTOR, "Ivan", "Instructor");
        cuenta(escuelaB, RolNombres.ESTUDIANTE, "Beto", "Beta");
        cuenta(escuelaB, RolNombres.ESTUDIANTE, "Berta", "Beta");
        cuenta(escuelaB, RolNombres.ESTUDIANTE, "Bruno", "Beta");

        mockMvc.perform(get(RUTA).with(como(adminA, "ADMIN_ESCUELA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ana Alfa"))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void ca02_ca03_ca04_buscaSinTildesFiltraPorEstadoYUnaBusquedaSinResultadosNoEsError() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        cuenta(escuela, RolNombres.ESTUDIANTE, "José", "Gómez");
        Usuario inactivo = cuenta(escuela, RolNombres.ESTUDIANTE, "Juan Carlos", "Pérez");
        jdbc.update("UPDATE usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?", inactivo.getCodigo());

        RequestPostProcessor sesion = como(admin, "ADMIN_ESCUELA");
        mockMvc.perform(get(RUTA).param("search", "jose").with(sesion))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("José Gómez"));
        mockMvc.perform(get(RUTA).param("search", "juan perez").with(sesion))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Juan Carlos Pérez"))
            .andExpect(jsonPath("$.data[0].estado").value("INACTIVO"));
        mockMvc.perform(get(RUTA).param("filter[estado]", "INACTIVO").with(sesion))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Juan Carlos Pérez"));
        mockMvc.perform(get(RUTA).param("filter[estado]", "ACTIVO").with(sesion))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("José Gómez"));
        mockMvc.perform(get(RUTA).param("search", "nadie").with(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0))
            .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void ca05_ca06_unEstudianteDeOtraEscuelaSeVeIgualQueUnoInexistente() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Usuario adminA = cuenta(escuelaA, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario propio = cuenta(escuelaA, RolNombres.ESTUDIANTE, "Ana", "Alfa");
        Usuario ajeno = cuenta(escuelaB, RolNombres.ESTUDIANTE, "Beto", "Beta");
        RequestPostProcessor sesion = como(adminA, "ADMIN_ESCUELA");

        mockMvc.perform(get(RUTA + "/" + propio.getCodigo()).with(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.edad").exists())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mockMvc.perform(get(RUTA + "/" + ajeno.getCodigo()).with(sesion))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_20"));
        mockMvc.perform(get(RUTA + "/" + UUID.randomUUID()).with(sesion))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_20"));
        // * Una cuenta de la misma escuela que no es de un estudiante tampoco se ve
        mockMvc.perform(get(RUTA + "/" + adminA.getCodigo()).with(sesion))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_20"));
    }

    @Test
    void ca07_elAdministradorCreaUnEstudianteEnSuEscuelaQueLuegoPuedeIniciarSesion() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Cañoto 100"));
        when(categorias.existe("B")).thenReturn(true);
        when(escuelas.buscarPorCodigo(escuela)).thenReturn(Optional.of(new EscuelaRef(escuela, "esc", "Esc", true)));
        String correo = "ca07-" + UUID.randomUUID() + "@gmail.com";

        crear(como(admin, "ADMIN_ESCUELA"), correo, CiDePrueba.unico(), ",\"categoriaSigla\":\"B\"")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.categoriaSigla").value("B"))
            .andExpect(jsonPath("$.data.direccion").value("Av. Cañoto 100"))
            .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT escuela_codigo::text FROM usuario WHERE email = ?", String.class, correo))
            .isEqualTo(escuela.toString());
        assertThat(jdbc.queryForObject("SELECT correo_verificado FROM usuario WHERE email = ?", Boolean.class, correo))
            .isFalse();
        assertThat(jdbc.queryForObject("SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
            + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = ?", String.class, correo)).isEqualTo("ESTUDIANTE");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + correo + "\",\"password\":\"123456789\",\"dispositivo\":\"MOVIL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void ca07_sinCategoriaQuedaEnSinCategoriaYSinConsultarEscuela() throws Exception {
        Usuario admin = cuenta(UUID.randomUUID(), RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        String correo = "ca07b-" + UUID.randomUUID() + "@gmail.com";

        crear(como(admin, "ADMIN_ESCUELA"), correo, CiDePrueba.unico(), "")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.categoriaSigla").value("SIN"));

        verify(categorias, never()).existe(any());
    }

    @Test
    void ca08_ca09_correoCiRepetidosMenorDeEdadYCategoriaInexistenteNoCreanNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario existente = cuenta(escuela, RolNombres.ESTUDIANTE, "Ana", "Alfa");
        RequestPostProcessor sesion = como(admin, "ADMIN_ESCUELA");
        String ciExistente = jdbc.queryForObject("SELECT p.ci FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.codigo_usuario = ?", String.class, existente.getCodigo());
        String correoNuevo = "ca08-" + UUID.randomUUID() + "@gmail.com";
        when(categorias.existe("ZZZ")).thenReturn(false);

        crear(sesion, existente.getEmail(), CiDePrueba.unico(), "")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(sesion, correoNuevo, ciExistente, "")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
        mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(correoNuevo, CiDePrueba.unico(), "")
                .replace("2000-01-01", LocalDate.now().minusYears(17).toString())))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
        crear(sesion, correoNuevo, CiDePrueba.unico(), ",\"categoriaSigla\":\"ZZZ\"")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_21"));

        assertThat(cuentasConCorreo(correoNuevo)).isZero();
    }

    @Test
    void ca10_elAdministradorSoloCambiaLaCategoriaYLaUbicacionYNadaMas() throws Exception {
        UUID escuela = UUID.randomUUID();
        Usuario admin = cuenta(escuela, RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario estudiante = cuenta(escuela, RolNombres.ESTUDIANTE, "Ana", "Alfa");
        when(categorias.existe("C")).thenReturn(true);
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Banzer 500"));
        String ciAntes = jdbc.queryForObject("SELECT p.ci FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.codigo_usuario = ?", String.class, estudiante.getCodigo());

        mockMvc.perform(patch(RUTA + "/" + estudiante.getCodigo()).with(como(admin, "ADMIN_ESCUELA"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoriaSigla\":\"c\",\"latitud\":-17.79,\"longitud\":-63.19,"
                + "\"ci\":\"HACK999\",\"nombres\":\"Otro\",\"email\":\"otro@gmail.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.categoriaSigla").value("C"))
            .andExpect(jsonPath("$.data.direccion").value("Av. Banzer 500"))
            .andExpect(jsonPath("$.data.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.ci").value(ciAntes));

        assertThat(jdbc.queryForObject("SELECT email FROM usuario WHERE codigo_usuario = ?", String.class,
            estudiante.getCodigo())).isEqualTo(estudiante.getEmail());
        assertThat(jdbc.queryForObject("SELECT p.categoria_sigla FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.codigo_usuario = ?", String.class, estudiante.getCodigo())).isEqualTo("C");
    }

    @Test
    void ca05_unAdministradorNoPuedeEditarAUnEstudianteDeOtraEscuela() throws Exception {
        Usuario adminA = cuenta(UUID.randomUUID(), RolNombres.ADMIN_ESCUELA, "Ada", "Admin");
        Usuario deB = cuenta(UUID.randomUUID(), RolNombres.ESTUDIANTE, "Beto", "Beta");

        mockMvc.perform(patch(RUTA + "/" + deB.getCodigo()).with(como(adminA, "ADMIN_ESCUELA"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"categoriaSigla\":\"SIN\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_20"));
    }
}
