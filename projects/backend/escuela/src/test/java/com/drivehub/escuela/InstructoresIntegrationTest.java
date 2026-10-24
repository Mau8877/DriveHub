package com.drivehub.escuela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.DatosCuentaInstructor;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

/**
 * CU09 en Escuela de punta a punta: controller, casos de uso con su @PreAuthorize, repositorio y
 * Postgres real, con Identidad simulada. Aquí se ejerce el ROL y el aislamiento por escuela, que los
 * tests unitarios no pueden cubrir. Cada método nombra el criterio de aceptación (CAxx).
 *
 * * Cada prueba usa sus propias escuelas (UUID nuevos) y licencias únicas: la base se comparte.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InstructoresIntegrationTest extends PostgresIntegrationTestBase {

    private static final String RUTA = "/api/v1/instructores";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InstructorRepository instructores;
    @Autowired
    private CategoriaLicenciaRepository categorias;

    @MockitoBean
    private InstructorCuentaGateway identidad;

    @BeforeEach
    void limpiarMock() {
        clearInvocations(identidad);
    }

    private static RequestPostProcessor como(UUID escuela, String rol) {
        return jwt().jwt(j -> j.claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static String licenciaUnica() {
        return "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Instructor ficha(UUID escuela, String licencia) {
        return instructores.guardar(Instructor.builder().codigo(UuidV7.generar()).usuarioCodigo(UUID.randomUUID())
            .escuelaCodigo(escuela).nroLicencia(licencia).licenciaVence(LocalDate.now().plusYears(2))
            .estado(EstadoInstructor.ACTIVO).fechaContratacion(LocalDate.of(2026, 1, 1))
            .categorias(new LinkedHashSet<>(categorias.buscarPorSiglas(List.of("B")))).build());
    }

    private static PersonaInstructor persona(UUID codigo, String nombres, String apellidos) {
        return new PersonaInstructor(codigo, nombres, apellidos, nombres + " " + apellidos, null, 36,
            LocalDate.of(1990, 1, 1), "MASCULINO", "AB12345", codigo + "@gmail.com", "70000000", "Av. Arce",
            new BigDecimal("-17.7833"), new BigDecimal("-63.1821"), LocalDate.now());
    }

    private static String cuerpoCrear(String licencia, String extra) {
        return """
            {"nombres":"Ivan","apellidos":"Pérez","fechaNacimiento":"1990-01-01","sexo":"MASCULINO","ci":"AB12345",
             "email":"ivan@gmail.com","password":"123456789","latitud":-17.7833,"longitud":-63.1821,
             "nroLicencia":"%s","licenciaVence":"%s","categorias":["A","B"]%s}
            """.formatted(licencia, LocalDate.now().plusYears(2), extra);
    }

    private ResultActions crear(RequestPostProcessor sesion, String licencia, String extra) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(licencia, extra)));
    }

    private int fichasConLicencia(String licencia) {
        return jdbc.queryForObject("SELECT count(*) FROM instructor WHERE nro_licencia = ?", Integer.class, licencia);
    }

    @Test
    void ca14_cualquierRolQueNoSeaAdministradorDeEscuelaRecibe403YNoSeCreaNiCambiaNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        Instructor existente = ficha(escuela, licenciaUnica());
        String licencia = licenciaUnica();

        for (String rol : new String[] {"ESTUDIANTE", "INSTRUCTOR", "SUPER_ADMIN"}) {
            RequestPostProcessor sesion = como(escuela, rol);
            mockMvc.perform(get(RUTA).with(sesion)).andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA + "/" + existente.getCodigo()).with(sesion)).andExpect(status().isForbidden());
            crear(sesion, licencia, "").andExpect(status().isForbidden());
            mockMvc.perform(patch(RUTA + "/" + existente.getCodigo()).with(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        assertThat(fichasConLicencia(licencia)).isZero();
        assertThat(instructores.buscarPorCodigo(escuela, existente.getCodigo()).orElseThrow().getEstado())
            .isEqualTo(EstadoInstructor.ACTIVO);
        verify(identidad, never()).crearCuenta(any());
    }

    @Test
    void ca01_ca05_elAdministradorListaSoloLosInstructoresDeSuEscuelaAunqueOtraTengaMas() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Instructor deA = ficha(escuelaA, licenciaUnica());
        ficha(escuelaB, licenciaUnica());
        ficha(escuelaB, licenciaUnica());
        when(identidad.consultar(any())).thenReturn(List.of(persona(deA.getUsuarioCodigo(), "Ivan", "Alfa")));

        mockMvc.perform(get(RUTA).with(como(escuelaA, "ADMIN_ESCUELA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].codigo").value(deA.getCodigo().toString()))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data[0].categorias[0].sigla").value("B"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
    }

    @Test
    void ca02_ca03_ca04_buscaPorNombreFiltraPorEstadoYUnaBusquedaSinResultadosNoEsError() throws Exception {
        UUID escuela = UUID.randomUUID();
        Instructor activo = ficha(escuela, licenciaUnica());
        Instructor deLicencia = ficha(escuela, licenciaUnica());
        jdbc.update("UPDATE instructor SET estado = 'LICENCIA' WHERE codigo_instructor = ?", deLicencia.getCodigo());
        RequestPostProcessor sesion = como(escuela, "ADMIN_ESCUELA");

        when(identidad.buscarCodigos("jose")).thenReturn(List.of(activo.getUsuarioCodigo()));
        when(identidad.consultar(List.of(activo.getUsuarioCodigo())))
            .thenReturn(List.of(persona(activo.getUsuarioCodigo(), "José", "Gómez")));
        mockMvc.perform(get(RUTA).param("search", "jose").with(sesion))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("José Gómez"));

        when(identidad.consultar(List.of(deLicencia.getUsuarioCodigo())))
            .thenReturn(List.of(persona(deLicencia.getUsuarioCodigo(), "Luis", "Paz")));
        mockMvc.perform(get(RUTA).param("filter[estado]", "LICENCIA").with(sesion))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].codigo").value(deLicencia.getCodigo().toString()))
            .andExpect(jsonPath("$.data[0].estado").value("LICENCIA"));

        when(identidad.buscarCodigos("nadie")).thenReturn(List.of());
        mockMvc.perform(get(RUTA).param("search", "nadie").with(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0))
            .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void ca05_ca06_unInstructorDeOtraEscuelaSeVeIgualQueUnoInexistenteAlVerloYAlEditarlo() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        UUID escuelaB = UUID.randomUUID();
        Instructor propio = ficha(escuelaA, licenciaUnica());
        Instructor ajeno = ficha(escuelaB, licenciaUnica());
        when(identidad.consultar(any())).thenReturn(List.of(persona(propio.getUsuarioCodigo(), "Ivan", "Alfa")));
        RequestPostProcessor sesion = como(escuelaA, "ADMIN_ESCUELA");

        mockMvc.perform(get(RUTA + "/" + propio.getCodigo()).with(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombres").value("Ivan"))
            .andExpect(jsonPath("$.data.ci").value("AB12345"))
            .andExpect(jsonPath("$.data.usuarioCodigo").doesNotExist());
        mockMvc.perform(get(RUTA + "/" + ajeno.getCodigo()).with(sesion))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        mockMvc.perform(get(RUTA + "/" + UUID.randomUUID()).with(sesion))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        mockMvc.perform(patch(RUTA + "/" + ajeno.getCodigo()).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        assertThat(instructores.buscarPorCodigo(escuelaB, ajeno.getCodigo()).orElseThrow().getEstado())
            .isEqualTo(EstadoInstructor.ACTIVO);
    }

    @Test
    void ca07_elAdministradorCreaLaFichaEnSuEscuelaConElCodigoDeLaPersonaQueSePidioAIdentidad() throws Exception {
        UUID escuela = UUID.randomUUID();
        String licencia = licenciaUnica();
        when(identidad.crearCuenta(any())).thenAnswer(inv -> {
            DatosCuentaInstructor datos = inv.getArgument(0);
            return persona(datos.usuarioCodigo(), datos.nombres(), datos.apellidos());
        });

        crear(como(escuela, "ADMIN_ESCUELA"), licencia.toLowerCase(), "")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.nroLicencia").value(licencia))
            .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
            .andExpect(jsonPath("$.data.categorias.length()").value(2))
            .andExpect(jsonPath("$.data.nombreCompleto").value("Ivan Pérez"))
            .andExpect(jsonPath("$.data.password").doesNotExist());

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        verify(identidad).crearCuenta(datos.capture());
        assertThat(jdbc.queryForObject("SELECT usuario_codigo FROM instructor WHERE nro_licencia = ?", UUID.class,
            licencia)).isEqualTo(datos.getValue().usuarioCodigo());
        assertThat(jdbc.queryForObject("SELECT escuela_codigo FROM instructor WHERE nro_licencia = ?", UUID.class,
            licencia)).isEqualTo(escuela);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM instructor_categoria ic JOIN instructor i "
            + "ON i.id = ic.instructor_id WHERE i.nro_licencia = ?", Integer.class, licencia)).isEqualTo(2);
    }

    @Test
    void ca09_unaLicenciaRepetidaResponde409SinLlamarAIdentidadNiCrearNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        String licencia = licenciaUnica();
        ficha(UUID.randomUUID(), licencia);

        crear(como(escuela, "ADMIN_ESCUELA"), licencia, "")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_08"));

        verify(identidad, never()).crearCuenta(any());
        assertThat(fichasConLicencia(licencia)).isEqualTo(1);
    }

    @Test
    void ca10_ca11_categoriaInvalidaOReglasDeFichaIncumplidasResponden400SinLlamarAIdentidad() throws Exception {
        RequestPostProcessor sesion = como(UUID.randomUUID(), "ADMIN_ESCUELA");

        mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCrear(licenciaUnica(), "").replace("[\"A\",\"B\"]", "[\"A\",\"ZZ\"]")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ESC_10"));
        mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCrear(licenciaUnica(), "").replace("[\"A\",\"B\"]", "[\"SIN\"]")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ESC_10"));
        mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCrear(licenciaUnica(), "").replace(LocalDate.now().plusYears(2).toString(),
                    LocalDate.now().toString())))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear(sesion, licenciaUnica(), ",\"fechaContratacion\":\"" + LocalDate.now().plusDays(1) + "\"")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(identidad, never()).crearCuenta(any());
    }

    @Test
    void ca12_siGuardarLaFichaFallaTrasCrearLaCuenta_seRevierteLaCuentaYNoQuedaFicha() throws Exception {
        UUID escuela = UUID.randomUUID();
        String licencia = licenciaUnica();
        // * Simula la carrera: mientras Identidad crea la cuenta, otro alta se queda con el mismo número
        when(identidad.crearCuenta(any())).thenAnswer(inv -> {
            DatosCuentaInstructor datos = inv.getArgument(0);
            jdbc.update("INSERT INTO instructor (codigo_instructor, usuario_codigo, escuela_codigo, nro_licencia, "
                + "estado) VALUES (?, ?, ?, ?, 'ACTIVO')", UuidV7.generar(), UUID.randomUUID(), escuela, licencia);
            return persona(datos.usuarioCodigo(), datos.nombres(), datos.apellidos());
        });

        crear(como(escuela, "ADMIN_ESCUELA"), licencia, "")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_08"));

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        verify(identidad).crearCuenta(datos.capture());
        verify(identidad).revertirCuenta(datos.getValue().usuarioCodigo());
        assertThat(fichasConLicencia(licencia)).isEqualTo(1);
    }

    @Test
    void ca12_siIdentidadNoResponde_seIntentaRevertirConElMismoCodigoYLaRespuestaEsUn500() throws Exception {
        when(identidad.crearCuenta(any())).thenThrow(new IdentidadNoDisponibleException("caída"));
        String licencia = licenciaUnica();

        crear(como(UUID.randomUUID(), "ADMIN_ESCUELA"), licencia, "")
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("ERR_SYS_00"));

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        verify(identidad).crearCuenta(datos.capture());
        verify(identidad).revertirCuenta(datos.getValue().usuarioCodigo());
        assertThat(fichasConLicencia(licencia)).isZero();
    }

    @Test
    void ca13_laEdicionCambiaSoloLaFichaYLaUbicacion() throws Exception {
        UUID escuela = UUID.randomUUID();
        Instructor original = ficha(escuela, licenciaUnica());
        when(identidad.consultar(any())).thenReturn(List.of(persona(original.getUsuarioCodigo(), "Ivan", "Alfa")));
        when(identidad.cambiarUbicacion(any(), any(), any())).thenReturn(
            persona(original.getUsuarioCodigo(), "Ivan", "Alfa"));
        String nueva = licenciaUnica();

        mockMvc.perform(patch(RUTA + "/" + original.getCodigo()).with(como(escuela, "ADMIN_ESCUELA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nroLicencia\":\"" + nueva + "\",\"estado\":\"LICENCIA\",\"categorias\":[\"C\",\"A\"],"
                    + "\"latitud\":-17.9,\"longitud\":-63.3,\"ci\":\"ZZ99999\",\"email\":\"otro@gmail.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nroLicencia").value(nueva))
            .andExpect(jsonPath("$.data.estado").value("LICENCIA"))
            .andExpect(jsonPath("$.data.categorias[0].sigla").value("A"))
            .andExpect(jsonPath("$.data.categorias[1].sigla").value("C"));

        verify(identidad).cambiarUbicacion(original.getUsuarioCodigo(), new BigDecimal("-17.9"), new BigDecimal("-63.3"));
        Instructor leido = instructores.buscarPorCodigo(escuela, original.getCodigo()).orElseThrow();
        assertThat(leido.getUsuarioCodigo()).isEqualTo(original.getUsuarioCodigo());
        assertThat(leido.getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(leido.getNroLicencia()).isEqualTo(nueva);
    }

    @Test
    void ca09_laEdicionConLaLicenciaDeOtroInstructorResponde409YNoCambiaNada() throws Exception {
        UUID escuela = UUID.randomUUID();
        Instructor uno = ficha(escuela, licenciaUnica());
        Instructor otro = ficha(escuela, licenciaUnica());

        mockMvc.perform(patch(RUTA + "/" + otro.getCodigo()).with(como(escuela, "ADMIN_ESCUELA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nroLicencia\":\"" + uno.getNroLicencia() + "\",\"estado\":\"INACTIVO\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_08"));

        Instructor leido = instructores.buscarPorCodigo(escuela, otro.getCodigo()).orElseThrow();
        assertThat(leido.getNroLicencia()).isEqualTo(otro.getNroLicencia());
        assertThat(leido.getEstado()).isEqualTo(EstadoInstructor.ACTIVO);
    }
}
