package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.FormatoImagen;
import com.drivehub.identidad.domain.model.ImagenInvalidaException;
import com.drivehub.identidad.domain.model.ImagenInvalidaException.Motivo;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.ImagenValidada;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.ValidadorImagenPort;

@ExtendWith(MockitoExtension.class)
class ActualizarFotoPerfilServiceTest {

    private static final byte[] FOTO = {1, 2, 3, 4};

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private ValidadorImagenPort validadorImagen;
    @Mock
    private AlmacenFotosPort almacenFotos;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;

    private ActualizarFotoPerfilService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ActualizarFotoPerfilService(usuarioRepository, validadorImagen, almacenFotos,
            new PerfilArmador(almacenFotos, escuelaConsulta), 100);
    }

    private Usuario cuenta(String fotoActual) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(EstadoUsuario.ACTIVO).fotoUrl(fotoActual).roles(Set.of()).build();
    }

    private void cuentaEncontrada(Usuario u) {
        when(usuarioRepository.buscarPorCodigo(u.getCodigo())).thenReturn(Optional.of(u));
    }

    private void assertRechazo(Usuario u, byte[] contenido, String codigo) {
        assertThatThrownBy(() -> servicio.actualizar(u.getCodigo(), contenido))
            .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(codigo));
        verify(almacenFotos, never()).guardar(anyString(), any(), anyString());
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void actualizar_conFotoValida_laGuardaConLaClaveEsperadaActualizaLaCuentaYBorraLaAnterior() {
        Usuario usuario = cuenta("usuarios/viejo/vieja.png");
        cuentaEncontrada(usuario);
        when(validadorImagen.validar(FOTO)).thenReturn(new ImagenValidada(FormatoImagen.PNG, 10, 10));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(almacenFotos.urlDeLectura(anyString())).thenReturn("http://minio/firmada");

        PerfilUsuario perfil = servicio.actualizar(usuario.getCodigo(), FOTO);

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(almacenFotos).guardar(clave.capture(), org.mockito.ArgumentMatchers.eq(FOTO),
            org.mockito.ArgumentMatchers.eq("image/png"));
        assertThat(clave.getValue()).matches("usuarios/" + usuario.getCodigo() + "/[0-9a-f-]{36}\\.png");
        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getFotoUrl()).isEqualTo(clave.getValue());
        verify(almacenFotos).borrar("usuarios/viejo/vieja.png");
        assertThat(perfil.fotoUrl()).isEqualTo("http://minio/firmada");
    }

    @Test
    void actualizar_sinFotoAnterior_noBorraNada() {
        Usuario usuario = cuenta(null);
        cuentaEncontrada(usuario);
        when(validadorImagen.validar(FOTO)).thenReturn(new ImagenValidada(FormatoImagen.WEBP, 10, 10));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(almacenFotos.urlDeLectura(anyString())).thenReturn("http://minio/firmada");

        servicio.actualizar(usuario.getCodigo(), FOTO);

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(almacenFotos).guardar(clave.capture(), any(), org.mockito.ArgumentMatchers.eq("image/webp"));
        assertThat(clave.getValue()).endsWith(".webp");
        verify(almacenFotos, never()).borrar(any());
    }

    @Test
    void actualizar_conMasDelMaximo_lanzaErrIde17SinTocarElAlmacenamiento() {
        Usuario usuario = cuenta(null);
        cuentaEncontrada(usuario);

        assertRechazo(usuario, new byte[101], IdentidadErrorCodes.FOTO_DEMASIADO_GRANDE);
        verifyNoInteractions(validadorImagen);
    }

    @Test
    void actualizar_conArchivoVacio_lanzaErrIde16() {
        Usuario usuario = cuenta(null);
        cuentaEncontrada(usuario);

        assertRechazo(usuario, new byte[0], IdentidadErrorCodes.FOTO_INVALIDA);
    }

    @Test
    void actualizar_conImagenRechazada_lanzaErrIde16ConUnMensajePorMotivo() {
        for (Motivo motivo : Motivo.values()) {
            Usuario usuario = cuenta(null);
            cuentaEncontrada(usuario);
            doThrow(new ImagenInvalidaException(motivo)).when(validadorImagen).validar(FOTO);

            assertThatThrownBy(() -> servicio.actualizar(usuario.getCodigo(), FOTO))
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.FOTO_INVALIDA);
                    assertThat(ex.getMessage()).isNotBlank();
                });
        }
        verify(almacenFotos, never()).guardar(anyString(), any(), anyString());
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void actualizar_conFormatoInvalido_elMensajeDiceQueNoEsUnaImagen() {
        Usuario usuario = cuenta(null);
        cuentaEncontrada(usuario);
        when(validadorImagen.validar(FOTO)).thenThrow(new ImagenInvalidaException(Motivo.FORMATO));

        assertThatThrownBy(() -> servicio.actualizar(usuario.getCodigo(), FOTO))
            .hasMessage("El archivo no es una imagen JPG, PNG o WebP");
    }

    @Test
    void actualizar_siLaBaseFalla_borraElObjetoNuevoYRelanza() {
        Usuario usuario = cuenta("usuarios/viejo/vieja.png");
        cuentaEncontrada(usuario);
        when(validadorImagen.validar(FOTO)).thenReturn(new ImagenValidada(FormatoImagen.JPEG, 10, 10));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> servicio.actualizar(usuario.getCodigo(), FOTO)).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> nueva = ArgumentCaptor.forClass(String.class);
        verify(almacenFotos).guardar(nueva.capture(), any(), anyString());
        verify(almacenFotos).borrar(nueva.getValue());
        verify(almacenFotos, never()).borrar("usuarios/viejo/vieja.png");
    }

    @Test
    void actualizar_siBorrarLaAnteriorFalla_igualResponde() {
        Usuario usuario = cuenta("usuarios/viejo/vieja.png");
        cuentaEncontrada(usuario);
        when(validadorImagen.validar(FOTO)).thenReturn(new ImagenValidada(FormatoImagen.PNG, 10, 10));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalStateException("MinIO caído")).when(almacenFotos).borrar("usuarios/viejo/vieja.png");
        when(almacenFotos.urlDeLectura(anyString())).thenReturn("http://minio/firmada");

        PerfilUsuario perfil = servicio.actualizar(usuario.getCodigo(), FOTO);

        assertThat(perfil.fotoUrl()).isEqualTo("http://minio/firmada");
    }

    @Test
    void actualizar_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(codigo, FOTO)).isInstanceOf(UnauthorizedException.class);
    }
}
