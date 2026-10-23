package com.drivehub.escuela.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.drivehub.escuela.application.dto.CrearEscuelaInput;
import com.drivehub.escuela.application.dto.EscuelaCreadaOutput;
import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.application.dto.ListarEscuelasInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.CrearEscuelaUseCase;
import com.drivehub.escuela.application.usecase.DesactivarEscuelaUseCase;
import com.drivehub.escuela.application.usecase.EditarEscuelaUseCase;
import com.drivehub.escuela.application.usecase.ListarEscuelasUseCase;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorCodigoUseCase;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorSlugUseCase;
import com.drivehub.escuela.application.usecase.ReactivarEscuelaUseCase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

@WebMvcTest({EscuelaGestionController.class, EscuelaController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EscuelaGestionControllerTest {

    private static final UUID CODIGO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/escuelas";
    private static final Instant ALTA = Instant.parse("2026-10-21T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearEscuelaUseCase crearEscuela;
    @MockitoBean
    private ListarEscuelasUseCase listarEscuelas;
    @MockitoBean
    private EditarEscuelaUseCase editarEscuela;
    @MockitoBean
    private DesactivarEscuelaUseCase desactivarEscuela;
    @MockitoBean
    private ReactivarEscuelaUseCase reactivarEscuela;
    @MockitoBean
    private ObtenerEscuelaPorSlugUseCase obtenerEscuelaPorSlug;
    @MockitoBean
    private ObtenerEscuelaPorCodigoUseCase obtenerEscuelaPorCodigo;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String cuerpoCrear() {
        return """
            {
              "nombre": "Autos del Sur",
              "slug": "autos-sur",
              "administrador": {
                "nombres": "Marta", "apellidos": "Gómez", "fechaNacimiento": "1985-05-05",
                "email": "marta@gmail.com", "password": "123456789", "telefono": "70000000",
                "ci": "MG98765", "sexo": "FEMENINO", "latitud": -16.4897, "longitud": -68.1193
              }
            }
            """;
    }

    private static EscuelaOutput escuela(String slug, EstadoEscuela estado) {
        return new EscuelaOutput(CODIGO, slug, "Autos del Sur", estado, ALTA);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------------------------ sin sesion

    @Test
    void todosLosEndpointsDeGestion_sinToken_responden401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + CODIGO).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA + "/" + CODIGO)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/reactivar")).andExpect(status().isUnauthorized());
    }

    @Test
    void laConsultaPublicaPorSlug_sigueRespondiendoSinToken() throws Exception {
        when(obtenerEscuelaPorSlug.ejecutar("escuelaexample"))
            .thenReturn(new com.drivehub.escuela.application.dto.EscuelaPublicaOutput(CODIGO, "escuelaexample", "Escuela Example"));

        mockMvc.perform(get(RUTA + "/escuelaexample")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_conDatosValidos_responde201ConLaEscuelaYElAdministradorSinContrasena() throws Exception {
        UUID adminCodigo = UUID.randomUUID();
        when(crearEscuela.ejecutar(any())).thenReturn(new EscuelaCreadaOutput(CODIGO, "autos-sur", "Autos del Sur",
            EstadoEscuela.ACTIVA, ALTA, new AdministradorCreado(adminCodigo, "marta@gmail.com", "Marta", "Gómez")));

        crear(cuerpoCrear())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
            .andExpect(jsonPath("$.data.administrador.codigo").value(adminCodigo.toString()))
            .andExpect(jsonPath("$.data.administrador.email").value("marta@gmail.com"))
            .andExpect(jsonPath("$.data.administrador.password").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<CrearEscuelaInput> input = ArgumentCaptor.forClass(CrearEscuelaInput.class);
        verify(crearEscuela).ejecutar(input.capture());
        assertThat(input.getValue().slug()).isEqualTo("autos-sur");
        assertThat(input.getValue().administrador().email()).isEqualTo("marta@gmail.com");
        // * CU08a: CI, sexo y ubicación del Administrador llegan al caso de uso
        assertThat(input.getValue().administrador().ci()).isEqualTo("MG98765");
        assertThat(input.getValue().administrador().sexo()).isEqualTo("FEMENINO");
        assertThat(input.getValue().administrador().latitud()).isEqualByComparingTo("-16.4897");
        assertThat(input.getValue().administrador().longitud()).isEqualByComparingTo("-68.1193");
    }

    @Test
    void crear_conCamposInvalidos_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        String[][] cambios = {
            {"\"slug\": \"autos-sur\"", "\"slug\": \"Autos Sur\""},
            {"\"slug\": \"autos-sur\"", "\"slug\": \"-autos\""},
            {"\"slug\": \"autos-sur\"", "\"slug\": \"ab\""},
            {"\"nombre\": \"Autos del Sur\"", "\"nombre\": \"ab\""},
            {"\"password\": \"123456789\"", "\"password\": \"12345678\""},
            {"marta@gmail.com", "no-es-correo"},
            {"1985-05-05", "2999-01-01"},
            {"\"ci\": \"MG98765\", ", ""},
            {"\"ci\": \"MG98765\"", "\"ci\": \"MG98\""},
            {"\"ci\": \"MG98765\"", "\"ci\": \"MG 987#\""},
            {"\"sexo\": \"FEMENINO\"", "\"sexo\": \"OTRO\""},
            {"\"latitud\": -16.4897, ", ""},
            {"\"latitud\": -16.4897", "\"latitud\": -91"},
            {"\"longitud\": -68.1193", "\"longitud\": 181"},
        };
        for (String[] cambio : cambios) {
            crear(cuerpoCrear().replace(cambio[0], cambio[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        crear("{\"nombre\": \"Autos\", \"slug\": \"autos\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear("esto no es json")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(crearEscuela, never()).ejecutar(any());
    }

    @Test
    void crear_losErroresDeNegocioLlegaronConSuHttpYSuCodigo() throws Exception {
        when(crearEscuela.ejecutar(any()))
            .thenThrow(new ConflictException(EscuelaErrorCodes.SLUG_EN_USO, "slug en uso"))
            .thenThrow(new ConflictException("ERR_IDE_01", "correo en uso"))
            .thenThrow(new com.drivehub.escuela.common.exception.ValidationException("ERR_IDE_02", "menor de edad"));

        crear(cuerpoCrear()).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_03"));
        crear(cuerpoCrear()).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crear(cuerpoCrear()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    // ------------------------------------------------------------------ listar

    @Test
    void listar_respondeLosElementosConMetaYPasaLosParametrosAlCasoDeUso() throws Exception {
        when(listarEscuelas.ejecutar(any())).thenReturn(new PaginaOutput<>(
            List.of(escuela("autos-sur", EstadoEscuela.ACTIVA), escuela("viejo", EstadoEscuela.INACTIVA)), 2, 10, 15, 2));

        mockMvc.perform(get(RUTA).with(jwt()).param("page", "2").param("pageSize", "10")
                .param("search", "sur").param("filter[estado]", "INACTIVA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].slug").value("autos-sur"))
            .andExpect(jsonPath("$.data[1].estado").value("INACTIVA"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(10))
            .andExpect(jsonPath("$.meta.totalItems").value(15))
            .andExpect(jsonPath("$.meta.totalPages").value(2));

        ArgumentCaptor<ListarEscuelasInput> input = ArgumentCaptor.forClass(ListarEscuelasInput.class);
        verify(listarEscuelas).ejecutar(input.capture());
        assertThat(input.getValue()).isEqualTo(new ListarEscuelasInput(2, 10, "sur", EstadoEscuela.INACTIVA));
    }

    @Test
    void listar_sinParametros_losPasaNulosParaQueElCasoDeUsoAplique_losValoresPorDefecto() throws Exception {
        when(listarEscuelas.ejecutar(any())).thenReturn(new PaginaOutput<>(List.of(), 1, 20, 0, 0));

        mockMvc.perform(get(RUTA).with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        ArgumentCaptor<ListarEscuelasInput> input = ArgumentCaptor.forClass(ListarEscuelasInput.class);
        verify(listarEscuelas).ejecutar(input.capture());
        assertThat(input.getValue()).isEqualTo(new ListarEscuelasInput(null, null, null, null));
    }

    @Test
    void listar_conUnEstadoOUnNumeroInvalidos_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt()).param("filter[estado]", "SUSPENDIDA"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(RUTA).with(jwt()).param("page", "uno"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(listarEscuelas, never()).ejecutar(any());
    }

    // ------------------------------------------------------------------ editar

    @Test
    void editar_conNombreValido_responde200ConLaEscuela() throws Exception {
        when(editarEscuela.ejecutar(CODIGO, "Nombre Nuevo")).thenReturn(
            new EscuelaOutput(CODIGO, "autos-sur", "Nombre Nuevo", EstadoEscuela.ACTIVA, ALTA));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre Nuevo\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Nombre Nuevo"))
            .andExpect(jsonPath("$.data.slug").value("autos-sur"));
    }

    @Test
    void editar_conNombreInvalidoOCodigoMalFormado_responde400SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"ab\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(patch(RUTA + "/no-es-uuid").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre Nuevo\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(editarEscuela, never()).ejecutar(any(), any());
    }

    @Test
    void editar_losErroresDeNegocioLlegaronConSuHttp() throws Exception {
        when(editarEscuela.ejecutar(any(), any()))
            .thenThrow(new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "no existe"))
            .thenThrow(new ConflictException(EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE, "inactiva"));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre Nuevo\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_02"));
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre Nuevo\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_06"));
    }

    // ------------------------------------------------------------------ desactivar y reactivar

    @Test
    void desactivar_responde200ConDataNula() throws Exception {
        mockMvc.perform(delete(RUTA + "/" + CODIGO).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist());
        verify(desactivarEscuela).ejecutar(CODIGO);
    }

    @Test
    void desactivar_siYaEstabaInactiva_responde409ErrEsc04() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException(EscuelaErrorCodes.ESCUELA_YA_INACTIVA, "ya inactiva"))
            .when(desactivarEscuela).ejecutar(any());

        mockMvc.perform(delete(RUTA + "/" + CODIGO).with(jwt()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_04"));
    }

    @Test
    void reactivar_responde200ConLaEscuelaActiva() throws Exception {
        when(reactivarEscuela.ejecutar(CODIGO)).thenReturn(escuela("autos-sur", EstadoEscuela.ACTIVA));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/reactivar").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.estado").value("ACTIVA"));
    }

    @Test
    void reactivar_siYaEstabaActiva_responde409ErrEsc05() throws Exception {
        when(reactivarEscuela.ejecutar(any()))
            .thenThrow(new ConflictException(EscuelaErrorCodes.ESCUELA_YA_ACTIVA, "ya activa"));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/reactivar").with(jwt()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_05"));
    }
}
