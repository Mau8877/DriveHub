package com.drivehub.escuela.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.escuela.application.dto.ListarVehiculosInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.dto.RegistrarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.application.usecase.CambiarEstadoVehiculoUseCase;
import com.drivehub.escuela.application.usecase.EditarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.FirmarSubidaImagenVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ListarVehiculosUseCase;
import com.drivehub.escuela.application.usecase.ObtenerVehiculoUseCase;
import com.drivehub.escuela.application.usecase.RegistrarVehiculoUseCase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.gateway.FirmaSubidaImagen;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

@WebMvcTest(VehiculoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VehiculoControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/vehiculos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarVehiculoUseCase registrarVehiculo;
    @MockitoBean
    private ListarVehiculosUseCase listarVehiculos;
    @MockitoBean
    private ObtenerVehiculoUseCase obtenerVehiculo;
    @MockitoBean
    private EditarVehiculoUseCase editarVehiculo;
    @MockitoBean
    private CambiarEstadoVehiculoUseCase cambiarEstado;
    @MockitoBean
    private FirmarSubidaImagenVehiculoUseCase firmarSubidaImagen;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    private static VehiculoOutput vehiculo(EstadoVehiculo estado) {
        return new VehiculoOutput(CODIGO, "ABC-123", "Toyota", "Yaris", (short) 2020, "Rojo", Transmision.MANUAL,
            estado, 1000, null, null, null, Instant.parse("2026-10-24T10:00:00Z"));
    }

    private static String cuerpoCrear() {
        return """
            {"placa":"abc-123","marca":"Toyota","modelo":"Yaris","anio":2020,"color":"Rojo",
             "transmision":"MANUAL","kilometraje":1000,"soatVence":"2027-01-01"}
            """;
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void todosLosEndpoints_sinToken_responden401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + CODIGO)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + CODIGO).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/habilitar")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrar_conDatosValidos_responde201TomandoLaEscuelaDelToken() throws Exception {
        when(registrarVehiculo.ejecutar(any())).thenReturn(vehiculo(EstadoVehiculo.DISPONIBLE));

        crear(cuerpoCrear())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<RegistrarVehiculoInput> input = ArgumentCaptor.forClass(RegistrarVehiculoInput.class);
        verify(registrarVehiculo).ejecutar(input.capture());
        assertThat(input.getValue().escuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(input.getValue().transmision()).isEqualTo(Transmision.MANUAL);
    }

    @Test
    void registrar_conCamposInvalidos_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        String[][] cambios = {
            {"\"placa\":\"abc-123\"", "\"placa\":\"ab\""},
            {"\"placa\":\"abc-123\"", "\"placa\":\"abc 123\""},
            {"\"marca\":\"Toyota\"", "\"marca\":\"\""},
            {"\"modelo\":\"Yaris\"", "\"modelo\":null"},
            {"\"transmision\":\"MANUAL\"", "\"transmision\":\"CVT\""},
            {"\"transmision\":\"MANUAL\"", "\"transmision\":null"},
            {"\"kilometraje\":1000", "\"kilometraje\":-1"},
            {"2027-01-01", "no-es-fecha"},
        };
        for (String[] cambio : cambios) {
            crear(cuerpoCrear().replace(cambio[0], cambio[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(registrarVehiculo, never()).ejecutar(any());
    }

    @Test
    void registrar_conTokenSinEscuela_responde403ErrEsc10() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(cuerpoCrear()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value(EscuelaErrorCodes.TOKEN_SIN_ESCUELA));
        verify(registrarVehiculo, never()).ejecutar(any());
    }

    @Test
    void registrar_conPlacaEnUso_responde409ErrEsc07() throws Exception {
        when(registrarVehiculo.ejecutar(any()))
            .thenThrow(new ConflictException(EscuelaErrorCodes.PLACA_EN_USO, "placa en uso"));

        crear(cuerpoCrear()).andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ESC_07"));
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(listarVehiculos.ejecutar(any())).thenReturn(new PaginaOutput<>(
            List.of(vehiculo(EstadoVehiculo.DISPONIBLE)), 2, 10, 11, 2));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10")
                .param("search", "yaris").param("filter[estado]", "BAJA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].placa").value("ABC-123"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(10))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(2));

        verify(listarVehiculos).ejecutar(new ListarVehiculosInput(ESCUELA, 2, 10, "yaris", EstadoVehiculo.BAJA));
    }

    @Test
    void listar_conEstadoDesconocido_responde400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[estado]", "ROTO"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_conAutoInexistente_responde404ErrEsc08() throws Exception {
        when(obtenerVehiculo.ejecutar(ESCUELA, CODIGO))
            .thenThrow(new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "no existe"));

        mockMvc.perform(get(RUTA + "/" + CODIGO).with(sesion()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_08"));
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(sesion()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void editar_pasaSoloLoQueLlegaYResponde200() throws Exception {
        when(editarVehiculo.ejecutar(any())).thenReturn(vehiculo(EstadoVehiculo.DISPONIBLE));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"Azul\",\"kilometraje\":1500}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()));

        verify(editarVehiculo).ejecutar(new com.drivehub.escuela.application.dto.EditarVehiculoInput(
            ESCUELA, CODIGO, null, null, null, "Azul", 1500, null, null, null));
    }

    @Test
    void editar_conKilometrajeNegativo_responde400SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kilometraje\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(editarVehiculo, never()).ejecutar(any());
    }

    @Test
    void lasTresAccionesDeEstado_llamanAlCasoDeUsoConSuEstadoDestino() throws Exception {
        when(cambiarEstado.ejecutar(eq(ESCUELA), eq(CODIGO), any())).thenAnswer(inv ->
            vehiculo(inv.getArgument(2)));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/enviar-a-mantenimiento").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("MANTENIMIENTO"));
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/habilitar").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("DISPONIBLE"));
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/dar-de-baja").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("BAJA"));
    }

    @Test
    void firmarImagen_devuelveLaFirmaDeLaEscuelaDelTokenSinElSecret() throws Exception {
        when(firmarSubidaImagen.ejecutar(ESCUELA)).thenReturn(
            new FirmaSubidaImagen("nube", "key", 1700000000L, "drivehub/" + ESCUELA + "/vehiculos", "abc123"));

        mockMvc.perform(post(RUTA + "/firma-imagen").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cloudName").value("nube"))
            .andExpect(jsonPath("$.data.apiKey").value("key"))
            .andExpect(jsonPath("$.data.timestamp").value(1700000000L))
            .andExpect(jsonPath("$.data.folder").value("drivehub/" + ESCUELA + "/vehiculos"))
            .andExpect(jsonPath("$.data.signature").value("abc123"))
            .andExpect(jsonPath("$.data.apiSecret").doesNotExist());
        mockMvc.perform(post(RUTA + "/firma-imagen")).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarEstado_conTransicionInvalida_responde409ErrEsc09() throws Exception {
        when(cambiarEstado.ejecutar(any(), any(), any()))
            .thenThrow(new ConflictException(EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA, "no se puede"));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/habilitar").with(sesion()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
    }
}
