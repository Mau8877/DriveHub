package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

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
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresComando;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.ConsultarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorComando;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorUseCase;
import com.drivehub.identidad.domain.port.in.EditarUbicacionInstructorUseCase;
import com.drivehub.identidad.domain.port.in.PaginaPersonasInstructor;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.in.RevertirCuentaInstructorUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(InstructorCuentaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class InstructorCuentaControllerTest {

    private static final String RUTA = "/api/v1/usuarios/instructores";
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearCuentaInstructorUseCase crearCuenta;
    @MockitoBean
    private ConsultarInstructoresUseCase consultarInstructores;
    @MockitoBean
    private BuscarInstructoresUseCase buscarInstructores;
    @MockitoBean
    private EditarUbicacionInstructorUseCase editarUbicacion;
    @MockitoBean
    private RevertirCuentaInstructorUseCase revertirCuenta;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(ADMIN.toString()));
    }

    private static PersonaInstructor persona() {
        return new PersonaInstructor(INSTRUCTOR, "Ivan", "Alfa", "Ivan Alfa", "http://minio/firmada", 36,
            LocalDate.of(1990, 1, 1), SexoPersona.MASCULINO, "AB12345", "ivan@gmail.com", "70000000",
            "Av. Cañoto 100", new BigDecimal("-17.783300"), new BigDecimal("-63.182100"), LocalDate.of(2026, 5, 4));
    }

    private static String cuerpoCrear(String extra) {
        return """
            {"codigo":"%s","nombres":"Ivan","apellidos":"Alfa","fechaNacimiento":"1990-01-01","sexo":"MASCULINO",
             "ci":"AB12345","telefono":"70000000","email":"ivan@gmail.com","password":"123456789",
             "latitud":-17.7833,"longitud":-63.1821%s}
            """.formatted(INSTRUCTOR, extra);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadaEndpoint_sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear("")))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/consulta").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigos\":[\"" + INSTRUCTOR + "\"]}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR + "/ubicacion").contentType(MediaType.APPLICATION_JSON)
            .content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA + "/" + INSTRUCTOR)).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_respondeCreatedConLaPersonaYElAdministradorDeLaSesion() throws Exception {
        when(crearCuenta.crear(any())).thenReturn(persona());

        crear(cuerpoCrear("")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.data.nombreCompleto").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data.fotoUrl").value("http://minio/firmada"))
            .andExpect(jsonPath("$.data.fechaAlta").value("2026-05-04"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.roles").doesNotExist())
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist());

        ArgumentCaptor<CrearCuentaInstructorComando> comando = ArgumentCaptor.forClass(CrearCuentaInstructorComando.class);
        verify(crearCuenta).crear(comando.capture());
        assertThat(comando.getValue().administradorCodigo()).isEqualTo(ADMIN);
        assertThat(comando.getValue().codigo()).isEqualTo(INSTRUCTOR);
    }

    @Test
    void crear_ignoraUnRolOUnaEscuelaEnElCuerpo() throws Exception {
        when(crearCuenta.crear(any())).thenReturn(persona());

        crear(cuerpoCrear(",\"rol\":\"SUPER_ADMIN\",\"escuelaCodigo\":\"" + UUID.randomUUID() + "\""))
            .andExpect(status().isCreated());

        ArgumentCaptor<CrearCuentaInstructorComando> comando = ArgumentCaptor.forClass(CrearCuentaInstructorComando.class);
        verify(crearCuenta).crear(comando.capture());
        assertThat(comando.getValue().administradorCodigo()).isEqualTo(ADMIN);
    }

    @Test
    void crear_conCamposInvalidos_responde400SinLlamarAlCasoDeUso() throws Exception {
        crear("{\"codigo\":\"no-es-uuid\"}").andExpect(status().isBadRequest());
        crear("{\"nombres\":\"Ivan\"}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear(cuerpoCrear("").replace("\"password\":\"123456789\"", "\"password\":\"corta\""))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(crearCuenta);
    }

    @Test
    void crear_losErroresDeNegocioLlegaConSuHttp() throws Exception {
        when(crearCuenta.crear(any()))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo"))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI"))
            .thenThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "Debes tener al menos 18 años"));

        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
        crear(cuerpoCrear("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    @Test
    void consultar_devuelveLasPersonasYPasaLosCodigos() throws Exception {
        when(consultarInstructores.consultar(any(), any())).thenReturn(List.of(persona()));

        mockMvc.perform(post(RUTA + "/consulta").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigos\":[\"" + INSTRUCTOR + "\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.data[0].ci").value("AB12345"));

        verify(consultarInstructores).consultar(ADMIN, List.of(INSTRUCTOR));
    }

    @Test
    void consultar_conCodigosVaciosOConMasDeCien_responde400SinLlamarAlCasoDeUso() throws Exception {
        String ciento1 = IntStream.range(0, 101).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
            .reduce((a, b) -> a + "," + b).orElseThrow();

        mockMvc.perform(post(RUTA + "/consulta").with(sesion()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigos\":[]}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/consulta").with(sesion()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigos\":[" + ciento1 + "]}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(consultarInstructores);
    }

    @Test
    void buscar_respondeElSobreConMetaYPasaLosParametros() throws Exception {
        when(buscarInstructores.buscar(any())).thenReturn(new PaginaPersonasInstructor(List.of(persona()), 2, 5, 11, 3));

        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "5").param("search", "ivan").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ivan Alfa"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(3));

        verify(buscarInstructores).buscar(new BuscarInstructoresComando(ADMIN, 2, 5, "ivan"));
    }

    @Test
    void editarUbicacion_cambiaLaUbicacionYPasaElInstructorDeLaUrl() throws Exception {
        when(editarUbicacion.editar(any(), any(), any(), any())).thenReturn(persona());

        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR + "/ubicacion").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codigo").value(INSTRUCTOR.toString()));

        verify(editarUbicacion).editar(ADMIN, INSTRUCTOR, new BigDecimal("-17.8"), new BigDecimal("-63.2"));
    }

    @Test
    void editarUbicacion_conDatosInvalidosOCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR + "/ubicacion").with(sesion())
            .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch(RUTA + "/no-es-uuid/ubicacion").with(sesion())
            .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(editarUbicacion);
    }

    @Test
    void editarUbicacion_conUnInstructorQueNoExiste_responde404ErrIde22() throws Exception {
        when(editarUbicacion.editar(any(), any(), any(), any())).thenThrow(
            new NotFoundException(IdentidadErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe"));

        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR + "/ubicacion").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-17.8,\"longitud\":-63.2}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_IDE_22"));
    }

    @Test
    void revertir_respondeOkConDataNula() throws Exception {
        mockMvc.perform(delete(RUTA + "/" + INSTRUCTOR).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(revertirCuenta).revertir(ADMIN, INSTRUCTOR);
    }

    @Test
    void revertir_conUnaCuentaYaUsada_responde409ErrIde23() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException(IdentidadErrorCodes.CUENTA_YA_USADA, "La cuenta ya se usó"))
            .when(revertirCuenta).revertir(any(), any());

        mockMvc.perform(delete(RUTA + "/" + INSTRUCTOR).with(sesion()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_23"));
    }

    @Test
    void revertir_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(delete(RUTA + "/abc").with(sesion())).andExpect(status().isBadRequest());

        verify(revertirCuenta, never()).revertir(any(), any());
    }
}
