package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.port.in.ActualizarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaComando;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaUseCase;
import com.drivehub.identidad.domain.port.in.EditarPerfilComando;
import com.drivehub.identidad.domain.port.in.EditarPerfilUseCase;
import com.drivehub.identidad.domain.port.in.ObtenerPerfilUseCase;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.in.QuitarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoComando;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(PerfilController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PerfilControllerTest {

    private static final String RUTA = "/api/v1/usuarios/me";
    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerPerfilUseCase obtenerPerfil;
    @MockitoBean
    private EditarPerfilUseCase editarPerfil;
    @MockitoBean
    private ActualizarFotoPerfilUseCase actualizarFoto;
    @MockitoBean
    private QuitarFotoPerfilUseCase quitarFoto;
    @MockitoBean
    private CambiarContrasenaUseCase cambiarContrasena;
    @MockitoBean
    private SolicitarCambioCorreoUseCase solicitarCambioCorreo;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(USUARIO.toString()));
    }

    private static PerfilUsuario perfil(String fotoUrl) {
        return new PerfilUsuario(USUARIO, "Ana", "Pérez", LocalDate.of(2000, 1, 1), "0991234567", "ana@gmail.com", true,
            List.of("ESTUDIANTE"), UUID.randomUUID(), "Autoescuela Sol", fotoUrl);
    }

    private static String json(String cuerpo) {
        return cuerpo;
    }

    @Test
    void obtener_devuelveElPerfilSinIdsInternosNiHash() throws Exception {
        when(obtenerPerfil.obtener(USUARIO)).thenReturn(perfil("http://minio/firmada"));

        mockMvc.perform(get(RUTA).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(USUARIO.toString()))
            .andExpect(jsonPath("$.data.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.email").value("ana@gmail.com"))
            .andExpect(jsonPath("$.data.correoVerificado").value(true))
            .andExpect(jsonPath("$.data.roles[0]").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.data.escuelaNombre").value("Autoescuela Sol"))
            .andExpect(jsonPath("$.data.fotoUrl").value("http://minio/firmada"))
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void obtener_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));

        verify(obtenerPerfil, never()).obtener(any());
    }

    @Test
    void editar_conLosCamposPresentes_armaElComandoConLaCuentaDeLaSesion() throws Exception {
        when(editarPerfil.editar(any())).thenReturn(perfil(null));

        mockMvc.perform(patch(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content(json("{\"nombres\":\"Luisa\",\"fechaNacimiento\":\"1995-05-05\",\"telefono\":\"0987654321\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombres").value("Ana"));

        ArgumentCaptor<EditarPerfilComando> comando = ArgumentCaptor.forClass(EditarPerfilComando.class);
        verify(editarPerfil).editar(comando.capture());
        assertThat(comando.getValue().usuarioCodigo()).isEqualTo(USUARIO);
        assertThat(comando.getValue().nombres()).contains("Luisa");
        assertThat(comando.getValue().apellidos()).isEmpty();
        assertThat(comando.getValue().telefono()).contains("0987654321");
        assertThat(comando.getValue().fechaNacimiento()).contains(LocalDate.of(1995, 5, 5));
    }

    @Test
    void editar_conTelefonoNuloExplicito_lopideBorrar_yAusenteNoLoToca() throws Exception {
        when(editarPerfil.editar(any())).thenReturn(perfil(null));

        mockMvc.perform(patch(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":null}"))
            .andExpect(status().isOk());
        mockMvc.perform(patch(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"Luisa\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<EditarPerfilComando> comando = ArgumentCaptor.forClass(EditarPerfilComando.class);
        verify(editarPerfil, org.mockito.Mockito.times(2)).editar(comando.capture());
        assertThat(comando.getAllValues().get(0).telefono()).isEqualTo(Optional.of(""));
        assertThat(comando.getAllValues().get(1).telefono()).isEmpty();
    }

    @Test
    void editar_conUnCampoDemasiadoLargoOVacio_responde400ErrSys01() throws Exception {
        String largo = "a".repeat(81);
        for (String cuerpo : new String[] {
            "{\"nombres\":\"" + largo + "\"}",
            "{\"apellidos\":\"" + largo + "\"}",
            "{\"nombres\":\"   \"}",
            "{\"telefono\":\"" + "1".repeat(21) + "\"}",
            "{\"fechaNacimiento\":\"2999-01-01\"}"}) {
            mockMvc.perform(patch(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(editarPerfil, never()).editar(any());
    }

    @Test
    void editar_conMenorDeEdad_responde400ErrIde02() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "Debes tener al menos 18 años"))
            .when(editarPerfil).editar(any());

        mockMvc.perform(patch(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaNacimiento\":\"2015-01-01\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    @Test
    void subirFoto_conMultipartValido_responde200ConElPerfil() throws Exception {
        byte[] contenido = {1, 2, 3};
        when(actualizarFoto.actualizar(eq(USUARIO), any())).thenReturn(perfil("http://minio/nueva"));

        mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", contenido)).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fotoUrl").value("http://minio/nueva"));

        verify(actualizarFoto).actualizar(USUARIO, contenido);
    }

    @Test
    void subirFoto_sinLaParteArchivo_responde400ErrSys01() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
                .file(new MockMultipartFile("otra", "foto.png", "image/png", new byte[] {1})).with(sesion()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(actualizarFoto, never()).actualizar(any(), any());
    }

    @Test
    void subirFoto_conUnaImagenRechazada_responde400ErrIde16() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.FOTO_INVALIDA, "El archivo no es una imagen JPG, PNG o WebP"))
            .when(actualizarFoto).actualizar(eq(USUARIO), any());

        mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1})).with(sesion()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_16"));
    }

    @Test
    void subirFoto_siSpringCortaPorTamano_responde400ErrIde17() throws Exception {
        doThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024L)).when(actualizarFoto).actualizar(eq(USUARIO), any());

        mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1})).with(sesion()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_17"));
    }

    @Test
    void subirFoto_sinToken_responde401() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, RUTA + "/foto")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1})))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void quitarFoto_responde200ConElPerfilSinFoto() throws Exception {
        when(quitarFoto.quitar(USUARIO)).thenReturn(perfil(null));

        mockMvc.perform(delete(RUTA + "/foto").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fotoUrl").doesNotExist());
    }

    @Test
    void cambiarContrasena_conDatosValidos_responde200SinData() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-contrasena").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"passwordNueva\":\"nueva-456-789\",\"dispositivo\":\"WEB\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(cambiarContrasena).cambiar(
            new CambiarContrasenaComando(USUARIO, "actual-123", "nueva-456-789", Dispositivo.WEB));
    }

    @Test
    void cambiarContrasena_conContrasenaCortaOSinDispositivo_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-contrasena").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"passwordNueva\":\"corta\",\"dispositivo\":\"WEB\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/cambiar-contrasena").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"passwordNueva\":\"nueva-456-789\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(cambiarContrasena, never()).cambiar(any());
    }

    @Test
    void cambiarContrasena_conLaActualIncorrecta_responde400ErrIde14() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA, "La contraseña actual no es correcta"))
            .when(cambiarContrasena).cambiar(any());

        mockMvc.perform(post(RUTA + "/cambiar-contrasena").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"mala\",\"passwordNueva\":\"nueva-456-789\",\"dispositivo\":\"MOVIL\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_14"));
    }

    @Test
    void cambiarContrasena_sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"a\",\"passwordNueva\":\"nueva-456-789\",\"dispositivo\":\"WEB\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarCorreo_conDatosValidos_responde200() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-correo").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"correoNuevo\":\"nuevo@gmail.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Te enviamos un correo a la dirección nueva"));

        verify(solicitarCambioCorreo).solicitar(new SolicitarCambioCorreoComando(USUARIO, "actual-123", "nuevo@gmail.com"));
    }

    @Test
    void cambiarCorreo_conCorreoSinFormatoOContrasenaVacia_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-correo").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"correoNuevo\":\"no-es-correo\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/cambiar-correo").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"\",\"correoNuevo\":\"nuevo@gmail.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(solicitarCambioCorreo, never()).solicitar(any());
    }

    @Test
    void cambiarCorreo_conCorreoEnUso_responde409ErrIde01() throws Exception {
        doThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo"))
            .when(solicitarCambioCorreo).solicitar(any());

        mockMvc.perform(post(RUTA + "/cambiar-correo").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"actual-123\",\"correoNuevo\":\"otro@gmail.com\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_01"));
    }

    @Test
    void cambiarCorreo_sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA + "/cambiar-correo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"a\",\"correoNuevo\":\"nuevo@gmail.com\"}"))
            .andExpect(status().isUnauthorized());
    }
}
