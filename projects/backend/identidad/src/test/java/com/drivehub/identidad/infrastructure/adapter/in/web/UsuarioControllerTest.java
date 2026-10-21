package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaComando;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaUseCase;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.RevertirAdministradorEscuelaUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarEstudianteUseCase registrarEstudiante;

    @MockitoBean
    private CrearAdministradorEscuelaUseCase crearAdministradorEscuela;

    @MockitoBean
    private RevertirAdministradorEscuelaUseCase revertirAdministradorEscuela;

    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String cuerpo(String campoExtra) {
        return """
            {
              "nombres": "Ana",
              "apellidos": "Pérez",
              "fechaNacimiento": "2000-01-01",
              "email": "ana@gmail.com",
              "password": "123456789",
              "escuelaSlug": "escuelaexample"%s
            }
            """.formatted(campoExtra);
    }

    private ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void registrar_conDatosValidos_responde201EnSobreSinContrasenaNiSesion() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(registrarEstudiante.registrar(any())).thenReturn(Usuario.builder()
            .codigo(codigo).email("ana@gmail.com").nombres("Ana").apellidos("Pérez").build());

        enviar(cuerpo(""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.email").value("ana@gmail.com"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.accessToken").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void registrar_conContrasenaDe8Caracteres_responde400ConErrSys01() throws Exception {
        enviar(cuerpo("").replace("123456789", "12345678"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(registrarEstudiante, never()).registrar(any());
    }

    @Test
    void registrar_conCorreoMalFormado_responde400ConErrSys01() throws Exception {
        enviar(cuerpo("").replace("ana@gmail.com", "no-es-un-correo"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_sinNombres_responde400ConErrSys01() throws Exception {
        enviar(cuerpo("").replace("\"nombres\": \"Ana\",", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_conFechaDeNacimientoFutura_responde400ConErrSys01() throws Exception {
        String futura = LocalDate.now().plusDays(1).toString();
        enviar(cuerpo("").replace("2000-01-01", futura))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_cuandoElServicioLanzaConflict_responde409ConErrIde01() throws Exception {
        when(registrarEstudiante.registrar(any()))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "correo en uso"));

        enviar(cuerpo(""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_01"));
    }

    @Test
    void registrar_cuandoElServicioLanzaMenorDeEdad_responde400ConErrIde02() throws Exception {
        when(registrarEstudiante.registrar(any()))
            .thenThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "menor"));

        enviar(cuerpo(""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    @Test
    void registrar_cuandoLaEscuelaNoExiste_responde404ConErrIde03() throws Exception {
        when(registrarEstudiante.registrar(any()))
            .thenThrow(new NotFoundException(IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE, "sin escuela"));

        enviar(cuerpo(""))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_IDE_03"));
    }

    @Test
    void registrar_conUnRolEnElJson_loIgnoraYNoLoPasaAlComando() throws Exception {
        when(registrarEstudiante.registrar(any())).thenReturn(Usuario.builder()
            .codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez").build());

        enviar(cuerpo(",\n  \"rol\": \"SUPER_ADMIN\"")).andExpect(status().isCreated());

        ArgumentCaptor<RegistrarEstudianteComando> comando = ArgumentCaptor.forClass(RegistrarEstudianteComando.class);
        verify(registrarEstudiante).registrar(comando.capture());
        assertThat(comando.getValue().toString()).doesNotContain("SUPER_ADMIN");
    }

    // ------------------------------------------------------------------ CU05: administradores de escuela

    private static final String RUTA_ADMIN = "/api/v1/usuarios/administradores-escuela";
    private static final UUID ESCUELA = UUID.randomUUID();

    private static String cuerpoAdmin() {
        return """
            {
              "nombres": "Marta",
              "apellidos": "Gómez",
              "fechaNacimiento": "1985-05-05",
              "email": "marta@gmail.com",
              "password": "123456789",
              "telefono": "70000000",
              "escuelaCodigo": "%s"
            }
            """.formatted(ESCUELA);
    }

    private ResultActions crearAdmin(String json) throws Exception {
        return mockMvc.perform(post(RUTA_ADMIN).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void crearAdministrador_sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA_ADMIN).contentType(MediaType.APPLICATION_JSON).content(cuerpoAdmin()))
            .andExpect(status().isUnauthorized());
        verify(crearAdministradorEscuela, never()).crear(any());
    }

    @Test
    void crearAdministrador_conDatosValidos_responde201ConLosDatosYSinContrasena() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(crearAdministradorEscuela.crear(any())).thenReturn(Usuario.builder().codigo(codigo)
            .email("marta@gmail.com").nombres("Marta").apellidos("Gómez").escuelaCodigo(ESCUELA).build());

        crearAdmin(cuerpoAdmin())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.escuelaCodigo").value(ESCUELA.toString()))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<CrearAdministradorEscuelaComando> comando =
            ArgumentCaptor.forClass(CrearAdministradorEscuelaComando.class);
        verify(crearAdministradorEscuela).crear(comando.capture());
        assertThat(comando.getValue().escuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(comando.getValue().email()).isEqualTo("marta@gmail.com");
    }

    @Test
    void crearAdministrador_conCamposInvalidos_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        crearAdmin(cuerpoAdmin().replace("123456789", "12345678"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crearAdmin(cuerpoAdmin().replace("marta@gmail.com", "no-es-correo"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crearAdmin(cuerpoAdmin().replace("\"escuelaCodigo\": \"" + ESCUELA + "\"", "\"escuelaCodigo\": null"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(crearAdministradorEscuela, never()).crear(any());
    }

    @Test
    void crearAdministrador_losErroresDeNegocioLlegaronConSuHttpYSuCodigo() throws Exception {
        when(crearAdministradorEscuela.crear(any()))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo"))
            .thenThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "Debe tener al menos 18 años"))
            .thenThrow(new ConflictException(IdentidadErrorCodes.ADMIN_YA_EXISTE_EN_ESCUELA, "La escuela ya tiene un administrador"));

        crearAdmin(cuerpoAdmin()).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
        crearAdmin(cuerpoAdmin()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_02"));
        crearAdmin(cuerpoAdmin()).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_12"));
    }

    @Test
    void revertirAdministrador_sinToken_responde401() throws Exception {
        mockMvc.perform(delete(RUTA_ADMIN).param("escuelaCodigo", ESCUELA.toString()))
            .andExpect(status().isUnauthorized());
        verify(revertirAdministradorEscuela, never()).revertir(any());
    }

    @Test
    void revertirAdministrador_conEscuelaCodigo_responde200ConDataNula() throws Exception {
        mockMvc.perform(delete(RUTA_ADMIN).with(jwt()).param("escuelaCodigo", ESCUELA.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Administrador revertido correctamente"));
        verify(revertirAdministradorEscuela).revertir(ESCUELA);
    }

    @Test
    void revertirAdministrador_sinONoUuid_responde400ErrSys01() throws Exception {
        mockMvc.perform(delete(RUTA_ADMIN).with(jwt()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(delete(RUTA_ADMIN).with(jwt()).param("escuelaCodigo", "no-es-uuid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(revertirAdministradorEscuela, never()).revertir(any());
    }

    @Test
    void revertirAdministrador_siYaIngreso_responde409ErrIde13() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException(IdentidadErrorCodes.ADMIN_EN_USO, "Ya inició sesión"))
            .when(revertirAdministradorEscuela).revertir(any());

        mockMvc.perform(delete(RUTA_ADMIN).with(jwt()).param("escuelaCodigo", ESCUELA.toString()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_13"));
    }
}
