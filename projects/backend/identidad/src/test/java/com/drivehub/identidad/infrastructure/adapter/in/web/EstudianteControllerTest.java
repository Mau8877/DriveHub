package com.drivehub.identidad.infrastructure.adapter.in.web;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.CrearEstudianteComando;
import com.drivehub.identidad.domain.port.in.CrearEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.EditarEstudianteComando;
import com.drivehub.identidad.domain.port.in.EditarEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.in.EstudianteResumen;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesComando;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesUseCase;
import com.drivehub.identidad.domain.port.in.ObtenerEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.PaginaEstudiantes;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(EstudianteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class EstudianteControllerTest {

    private static final String RUTA = "/api/v1/estudiantes";
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarEstudiantesUseCase listarEstudiantes;
    @MockitoBean
    private ObtenerEstudianteUseCase obtenerEstudiante;
    @MockitoBean
    private CrearEstudianteUseCase crearEstudiante;
    @MockitoBean
    private EditarEstudianteUseCase editarEstudiante;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(ADMIN.toString()));
    }

    private static EstudianteDetalle detalle() {
        return new EstudianteDetalle(ESTUDIANTE, "Ana", "Alfa", "Ana Alfa", 26, LocalDate.of(2000, 1, 1),
            SexoPersona.FEMENINO, "AB12345", "ana@gmail.com", "70000000", "Av. Cañoto 100",
            new BigDecimal("-17.783300"), new BigDecimal("-63.182100"), "SIN", EstadoEstudiante.ACTIVO,
            LocalDate.of(2026, 5, 4));
    }

    private static String cuerpoCrear(String extra) {
        return """
            {"nombres":"Ana","apellidos":"Alfa","fechaNacimiento":"2000-01-01","sexo":"FEMENINO","ci":"AB12345",
             "telefono":"70000000","email":"ana@gmail.com","password":"123456789",
             "latitud":-17.7833,"longitud":-63.1821%s}
            """.formatted(extra);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions editar(String json) throws Exception {
        return mockMvc.perform(patch(RUTA + "/" + ESTUDIANTE).with(sesion())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadaEndpoint_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + ESTUDIANTE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear("")))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + ESTUDIANTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_respondeElSobreConMetaYElAdministradorDeLaSesion() throws Exception {
        when(listarEstudiantes.listar(any())).thenReturn(new PaginaEstudiantes(List.of(
            new EstudianteResumen(ESTUDIANTE, "Ana Alfa", 26, "SIN", EstadoEstudiante.ACTIVO,
                LocalDate.of(2026, 5, 4))), 2, 5, 11, 3));

        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "5").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(ESTUDIANTE.toString()))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ana Alfa"))
            .andExpect(jsonPath("$.data[0].edad").value(26))
            .andExpect(jsonPath("$.data[0].categoriaSigla").value("SIN"))
            .andExpect(jsonPath("$.data[0].estado").value("ACTIVO"))
            .andExpect(jsonPath("$.data[0].fechaIncorporacion").value("2026-05-04"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(3));
    }

    @Test
    void listar_pasaPageSizeSearchYFiltroDeEstadoCodificadoAlCasoDeUso() throws Exception {
        when(listarEstudiantes.listar(any())).thenReturn(new PaginaEstudiantes(List.of(), 1, 20, 0, 0));

        // * El servidor entrega el parámetro ya decodificado: `filter%5Bestado%5D` llega como `filter[estado]`
        mockMvc.perform(get(RUTA).param("page", "3").param("pageSize", "7").param("search", "juan perez")
            .param("filter[estado]", "INACTIVO").with(sesion()))
            .andExpect(status().isOk());

        ArgumentCaptor<ListarEstudiantesComando> comando = ArgumentCaptor.forClass(ListarEstudiantesComando.class);
        verify(listarEstudiantes).listar(comando.capture());
        assertThat(comando.getValue()).isEqualTo(new ListarEstudiantesComando(
            ADMIN, 3, 7, "juan perez", EstadoFiltroEstudiante.INACTIVO));
    }

    @Test
    void listar_conUnEstadoQueNoExiste_responde400SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[estado]", "XYZ").with(sesion()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(listarEstudiantes, never()).listar(any());
    }

    @Test
    void obtener_respondeLaFichaSinIdsInternosNiHashNiRoles() throws Exception {
        when(obtenerEstudiante.obtener(ADMIN, ESTUDIANTE)).thenReturn(detalle());

        mockMvc.perform(get(RUTA + "/" + ESTUDIANTE).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codigo").value(ESTUDIANTE.toString()))
            .andExpect(jsonPath("$.data.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.nombreCompleto").value("Ana Alfa"))
            .andExpect(jsonPath("$.data.fechaNacimiento").value("2000-01-01"))
            .andExpect(jsonPath("$.data.sexo").value("FEMENINO"))
            .andExpect(jsonPath("$.data.ci").value("AB12345"))
            .andExpect(jsonPath("$.data.email").value("ana@gmail.com"))
            .andExpect(jsonPath("$.data.direccion").value("Av. Cañoto 100"))
            .andExpect(jsonPath("$.data.latitud").value(-17.7833))
            .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.roles").doesNotExist())
            .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void obtener_conUnEstudianteInexistente_responde404ConErrIde20() throws Exception {
        when(obtenerEstudiante.obtener(ADMIN, ESTUDIANTE)).thenThrow(
            new NotFoundException(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO, "El estudiante no existe"));

        mockMvc.perform(get(RUTA + "/" + ESTUDIANTE).with(sesion()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_20"));
    }

    @Test
    void obtener_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(sesion()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_conDatosValidos_responde201ConLaFichaYArmaElComandoConElAdministradorDeLaSesion() throws Exception {
        when(crearEstudiante.crear(any())).thenReturn(detalle());

        crear(cuerpoCrear(",\"categoriaSigla\":\"B\""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(ESTUDIANTE.toString()))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<CrearEstudianteComando> comando = ArgumentCaptor.forClass(CrearEstudianteComando.class);
        verify(crearEstudiante).crear(comando.capture());
        assertThat(comando.getValue().administradorCodigo()).isEqualTo(ADMIN);
        assertThat(comando.getValue().categoriaSigla()).isEqualTo("B");
        assertThat(comando.getValue().email()).isEqualTo("ana@gmail.com");
    }

    @Test
    void crear_ignoraElRolLaEscuelaYLaDireccionDelCuerpo() throws Exception {
        when(crearEstudiante.crear(any())).thenReturn(detalle());

        crear(cuerpoCrear(",\"rol\":\"SUPER_ADMIN\",\"escuelaCodigo\":\"" + UUID.randomUUID()
            + "\",\"direccion\":\"inventada\",\"administradorCodigo\":\"" + UUID.randomUUID() + "\""))
            .andExpect(status().isCreated());

        ArgumentCaptor<CrearEstudianteComando> comando = ArgumentCaptor.forClass(CrearEstudianteComando.class);
        verify(crearEstudiante).crear(comando.capture());
        assertThat(comando.getValue().administradorCodigo()).isEqualTo(ADMIN);
    }

    @Test
    void crear_conCamposInvalidos_responde400SinLlamarAlCasoDeUso() throws Exception {
        String[] invalidos = {
            cuerpoCrear("").replace("\"email\":\"ana@gmail.com\"", "\"email\":\"no-es-correo\""),
            cuerpoCrear("").replace("\"password\":\"123456789\"", "\"password\":\"12345678\""),
            cuerpoCrear("").replace("\"ci\":\"AB12345\"", "\"ci\":\"AB 1\""),
            cuerpoCrear("").replace("\"sexo\":\"FEMENINO\"", "\"sexo\":\"OTRO\""),
            cuerpoCrear("").replace("\"latitud\":-17.7833", "\"latitud\":-91"),
            cuerpoCrear("").replace("\"nombres\":\"Ana\"", "\"nombres\":\"  \""),
            cuerpoCrear("").replace("\"fechaNacimiento\":\"2000-01-01\"", "\"fechaNacimiento\":\"2999-01-01\""),
            cuerpoCrear("").replace("\"latitud\":-17.7833,\"longitud\":-63.1821", "\"sinUbicacion\":true"),
            cuerpoCrear(",\"categoriaSigla\":\"ABCD\""),
        };
        for (String json : invalidos) {
            crear(json).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(crearEstudiante, never()).crear(any());
    }

    @Test
    void crear_propagaLosErroresDelCasoDeUsoConSuHttp() throws Exception {
        when(crearEstudiante.crear(any()))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo"))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI"))
            .thenThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "Debes tener al menos 18 años"))
            .thenThrow(new ValidationException(IdentidadErrorCodes.CATEGORIA_NO_EXISTE, "La categoría no existe"));

        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
        crear(cuerpoCrear("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
        crear(cuerpoCrear("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_21"));
    }

    @Test
    void editar_conCategoriaYUbicacion_responde200YArmaElComando() throws Exception {
        when(editarEstudiante.editar(any())).thenReturn(detalle());

        editar("{\"categoriaSigla\":\"B\",\"latitud\":-17.79,\"longitud\":-63.19}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(ESTUDIANTE.toString()));

        ArgumentCaptor<EditarEstudianteComando> comando = ArgumentCaptor.forClass(EditarEstudianteComando.class);
        verify(editarEstudiante).editar(comando.capture());
        assertThat(comando.getValue().administradorCodigo()).isEqualTo(ADMIN);
        assertThat(comando.getValue().estudianteCodigo()).isEqualTo(ESTUDIANTE);
        assertThat(comando.getValue().categoriaSigla()).isEqualTo("B");
        assertThat(comando.getValue().latitud()).isEqualByComparingTo("-17.79");
    }

    @Test
    void editar_conOtrosCamposEnElCuerpo_losIgnoraYNoLosPasaAlComando() throws Exception {
        when(editarEstudiante.editar(any())).thenReturn(detalle());

        editar("{\"categoriaSigla\":\"B\",\"ci\":\"ZZ99999\",\"nombres\":\"Otro\",\"email\":\"otro@gmail.com\","
            + "\"password\":\"x\",\"estado\":\"INACTIVO\"}")
            .andExpect(status().isOk());

        ArgumentCaptor<EditarEstudianteComando> comando = ArgumentCaptor.forClass(EditarEstudianteComando.class);
        verify(editarEstudiante).editar(comando.capture());
        // * El comando ni siquiera tiene dónde llevar el CI ni los nombres: solo estos tres campos
        assertThat(comando.getValue()).isEqualTo(new EditarEstudianteComando(ADMIN, ESTUDIANTE, "B", null, null));
    }

    @Test
    void editar_conCoordenadasFueraDeRango_responde400() throws Exception {
        editar("{\"latitud\":95,\"longitud\":-63.19}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(editarEstudiante, never()).editar(any());
    }

    @Test
    void editar_propagaErrIde20YErrIde21() throws Exception {
        when(editarEstudiante.editar(any()))
            .thenThrow(new NotFoundException(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO, "El estudiante no existe"))
            .thenThrow(new ValidationException(IdentidadErrorCodes.CATEGORIA_NO_EXISTE, "La categoría no existe"));

        editar("{\"categoriaSigla\":\"B\"}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_IDE_20"));
        editar("{\"categoriaSigla\":\"ZZZ\"}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_21"));
    }
}
