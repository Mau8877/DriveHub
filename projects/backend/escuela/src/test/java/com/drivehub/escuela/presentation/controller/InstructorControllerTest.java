package com.drivehub.escuela.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.drivehub.escuela.application.dto.CategoriaOutput;
import com.drivehub.escuela.application.dto.CrearInstructorInput;
import com.drivehub.escuela.application.dto.EditarInstructorInput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.application.dto.ListarInstructoresInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.CrearInstructorUseCase;
import com.drivehub.escuela.application.usecase.EditarInstructorUseCase;
import com.drivehub.escuela.application.usecase.ListarInstructoresUseCase;
import com.drivehub.escuela.application.usecase.ObtenerInstructorUseCase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;
import com.drivehub.escuela.infrastructure.config.WebMvcConfig;

@WebMvcTest(InstructorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class InstructorControllerTest {

    private static final String RUTA = "/api/v1/instructores";
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarInstructoresUseCase listar;
    @MockitoBean
    private ObtenerInstructorUseCase obtener;
    @MockitoBean
    private CrearInstructorUseCase crear;
    @MockitoBean
    private EditarInstructorUseCase editar;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    private static InstructorDetalleOutput detalle() {
        return new InstructorDetalleOutput(INSTRUCTOR, "Ivan", "Alfa", "Ivan Alfa", "http://minio/foto", 36,
            LocalDate.of(1990, 1, 1), "MASCULINO", "AB12345", "ivan@gmail.com", "70000000", "Av. Arce",
            new BigDecimal("-17.78"), new BigDecimal("-63.18"), "LIC-1", LocalDate.of(2030, 1, 1),
            LocalDate.of(2026, 1, 1), List.of(new CategoriaOutput("A", "Profesional A")), EstadoInstructor.ACTIVO,
            LocalDate.of(2026, 5, 4));
    }

    private static String cuerpoCrear(String extra) {
        return """
            {"nombres":"Ivan","apellidos":"Alfa","fechaNacimiento":"1990-01-01","sexo":"MASCULINO","ci":"AB12345",
             "telefono":"70000000","email":"ivan@gmail.com","password":"123456789","latitud":-17.7833,
             "longitud":-63.1821,"nroLicencia":"LIC-1","licenciaVence":"2030-01-01","categorias":["A"]%s}
            """.formatted(extra);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadaEndpoint_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + INSTRUCTOR)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear("")))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void conSesionSinClaimDeEscuela_responde403SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.claim("escuela", "no-es-uuid"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA + "/" + INSTRUCTOR).with(jwt())).andExpect(status().isForbidden());

        verifyNoInteractions(listar, obtener, crear, editar);
    }

    @Test
    void listar_respondeElSobreConMetaYLaEscuelaDelToken() throws Exception {
        when(listar.ejecutar(any())).thenReturn(new PaginaOutput<>(List.of(new InstructorResumenOutput(INSTRUCTOR,
            "Ivan Alfa", "http://minio/foto", "LIC-1", LocalDate.of(2030, 1, 1),
            List.of(new CategoriaOutput("A", "Profesional A")), EstadoInstructor.ACTIVO)), 2, 5, 11, 3));

        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "5").param("search", "ivan")
                .param("filter[estado]", "ACTIVO").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data[0].licenciaVence").value("2030-01-01"))
            .andExpect(jsonPath("$.data[0].categorias[0].sigla").value("A"))
            .andExpect(jsonPath("$.data[0].estado").value("ACTIVO"))
            .andExpect(jsonPath("$.data[0].usuarioCodigo").doesNotExist())
            .andExpect(jsonPath("$.data[0].escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(3));

        ArgumentCaptor<ListarInstructoresInput> input = ArgumentCaptor.forClass(ListarInstructoresInput.class);
        verify(listar).ejecutar(input.capture());
        assertThat(input.getValue()).isEqualTo(new ListarInstructoresInput(ESCUELA, 2, 5, "ivan", EstadoInstructor.ACTIVO));
    }

    @Test
    void listar_conEstadoQueNoExiste_responde400SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[estado]", "XYZ").with(sesion()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(listar);
    }

    @Test
    void obtener_devuelveElDetalleSinDatosInternos() throws Exception {
        when(obtener.ejecutar(ESCUELA, INSTRUCTOR)).thenReturn(detalle());

        mockMvc.perform(get(RUTA + "/" + INSTRUCTOR).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.data.ci").value("AB12345"))
            .andExpect(jsonPath("$.data.fechaNacimiento").value("1990-01-01"))
            .andExpect(jsonPath("$.data.fechaAlta").value("2026-05-04"))
            .andExpect(jsonPath("$.data.categorias[0].descripcion").value("Profesional A"))
            .andExpect(jsonPath("$.data.usuarioCodigo").doesNotExist())
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void obtener_conCodigoQueNoEsUuid_responde400YConInstructorInexistente_responde404ErrEsc09() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").with(sesion())).andExpect(status().isBadRequest());

        when(obtener.ejecutar(any(), any())).thenThrow(
            new NotFoundException(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe"));
        mockMvc.perform(get(RUTA + "/" + INSTRUCTOR).with(sesion()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_13"));
    }

    @Test
    void crear_respondeCreatedYPasaLaEscuelaDelToken() throws Exception {
        when(crear.ejecutar(any())).thenReturn(detalle());

        crear(cuerpoCrear("")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.message").value("Instructor creado correctamente"));

        ArgumentCaptor<CrearInstructorInput> input = ArgumentCaptor.forClass(CrearInstructorInput.class);
        verify(crear).ejecutar(input.capture());
        assertThat(input.getValue().escuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(input.getValue().nroLicencia()).isEqualTo("LIC-1");
        assertThat(input.getValue().categorias()).containsExactly("A");
        assertThat(input.getValue().fechaContratacion()).isNull();
    }

    @Test
    void crear_ignoraUnaEscuelaOUnRolEnElCuerpo() throws Exception {
        when(crear.ejecutar(any())).thenReturn(detalle());

        crear(cuerpoCrear(",\"escuelaCodigo\":\"" + UUID.randomUUID() + "\",\"rol\":\"SUPER_ADMIN\""))
            .andExpect(status().isCreated());

        ArgumentCaptor<CrearInstructorInput> input = ArgumentCaptor.forClass(CrearInstructorInput.class);
        verify(crear).ejecutar(input.capture());
        assertThat(input.getValue().escuelaCodigo()).isEqualTo(ESCUELA);
    }

    @Test
    void crear_conCamposInvalidos_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        crear("{\"nombres\":\"Ivan\"}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear(cuerpoCrear("").replace("\"categorias\":[\"A\"]", "\"categorias\":[]"))
            .andExpect(status().isBadRequest());
        crear(cuerpoCrear("").replace("MASCULINO", "OTRO")).andExpect(status().isBadRequest());
        crear(cuerpoCrear("").replace("\"LIC-1\"", "\"AB\"")).andExpect(status().isBadRequest());
        crear(cuerpoCrear("").replace("\"licenciaVence\":\"2030-01-01\",", "")).andExpect(status().isBadRequest());
        crear(cuerpoCrear("").replace("123456789", "corta")).andExpect(status().isBadRequest());

        verifyNoInteractions(crear);
    }

    @Test
    void crear_losErroresDeNegocioLlegaConSuHttpYSuCodigo() throws Exception {
        when(crear.ejecutar(any()))
            .thenThrow(new ConflictException("ERR_IDE_01", "Ya existe una cuenta con ese correo"))
            .thenThrow(new ConflictException("ERR_IDE_19", "Ya existe una cuenta con ese CI"))
            .thenThrow(new ValidationException("ERR_IDE_02", "Debes tener al menos 18 años"))
            .thenThrow(new ConflictException(EscuelaErrorCodes.NRO_LICENCIA_EN_USO, "Licencia en uso"))
            .thenThrow(new ValidationException(EscuelaErrorCodes.CATEGORIA_INVALIDA, "Categoría inválida"));

        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
        crear(cuerpoCrear("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
        crear(cuerpoCrear("")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_12"));
        crear(cuerpoCrear("")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ESC_14"));
    }

    @Test
    void editar_pasaSoloLaFichaYLaUbicacionYIgnoraLosDatosPersonales() throws Exception {
        when(editar.ejecutar(any())).thenReturn(detalle());

        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"estado":"LICENCIA","nroLicencia":"NUEVA-9","categorias":["B"],"latitud":-17.8,"longitud":-63.2,
                     "ci":"ZZ99999","email":"otro@gmail.com","nombres":"Otro","escuelaCodigo":"%s"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Instructor actualizado correctamente"));

        ArgumentCaptor<EditarInstructorInput> input = ArgumentCaptor.forClass(EditarInstructorInput.class);
        verify(editar).ejecutar(input.capture());
        assertThat(input.getValue()).isEqualTo(new EditarInstructorInput(ESCUELA, INSTRUCTOR, "NUEVA-9", null, null,
            List.of("B"), EstadoInstructor.LICENCIA, new BigDecimal("-17.8"), new BigDecimal("-63.2")));
    }

    @Test
    void editar_conDatosInvalidos_responde400() throws Exception {
        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR).with(sesion()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"estado\":\"XYZ\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR).with(sesion()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitud\":500}")).andExpect(status().isBadRequest());
        mockMvc.perform(patch(RUTA + "/abc").with(sesion()).contentType(MediaType.APPLICATION_JSON)
            .content("{}")).andExpect(status().isBadRequest());

        verifyNoInteractions(editar);
    }

    @Test
    void editar_conInstructorInexistente_responde404ErrEsc09() throws Exception {
        when(editar.ejecutar(any())).thenThrow(
            new NotFoundException(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO, "El instructor no existe"));

        mockMvc.perform(patch(RUTA + "/" + INSTRUCTOR).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ACTIVO\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_13"));
    }
}
