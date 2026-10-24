package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class QuitarFotoPerfilServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private AlmacenFotosPort almacenFotos;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;

    private QuitarFotoPerfilService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new QuitarFotoPerfilService(usuarioRepository, almacenFotos,
            new PerfilArmador(almacenFotos, escuelaConsulta));
    }

    private Usuario cuenta(String foto) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(EstadoUsuario.ACTIVO).fotoUrl(foto).roles(Set.of()).build();
    }

    @Test
    void quitar_conFoto_limpiaLaCuentaYBorraElObjeto() {
        Usuario usuario = cuenta("usuarios/u/a.png");
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        PerfilUsuario perfil = servicio.quitar(usuario.getCodigo());

        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getFotoUrl()).isNull();
        verify(almacenFotos).borrar("usuarios/u/a.png");
        assertThat(perfil.fotoUrl()).isNull();
    }

    @Test
    void quitar_sinFoto_noHaceNadaYNoEsError() {
        Usuario usuario = cuenta(null);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        PerfilUsuario perfil = servicio.quitar(usuario.getCodigo());

        assertThat(perfil.fotoUrl()).isNull();
        verify(usuarioRepository, never()).actualizar(any());
        verify(almacenFotos, never()).borrar(any());
    }

    @Test
    void quitar_siBorrarElObjetoFalla_igualResponde() {
        Usuario usuario = cuenta("usuarios/u/a.png");
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalStateException("MinIO caído")).when(almacenFotos).borrar("usuarios/u/a.png");

        PerfilUsuario perfil = servicio.quitar(usuario.getCodigo());

        assertThat(perfil.fotoUrl()).isNull();
    }
}
