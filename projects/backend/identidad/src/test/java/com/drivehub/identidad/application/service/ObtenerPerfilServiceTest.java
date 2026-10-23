package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ObtenerPerfilServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private AlmacenFotosPort almacenFotos;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;

    private ObtenerPerfilService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ObtenerPerfilService(usuarioRepository, new PerfilArmador(almacenFotos, escuelaConsulta));
    }

    private Usuario.UsuarioBuilder cuenta() {
        Rol estudiante = Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.ESTUDIANTE).permisos(Set.of()).build();
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").passwordHash("$2a$secreto")
            .nombres("Ana").apellidos("Pérez").telefono("0991234567").fechaNacimiento(LocalDate.of(2000, 1, 1))
            .escuelaCodigo(ESCUELA).estado(EstadoUsuario.ACTIVO).correoVerificado(true).roles(Set.of(estudiante));
    }

    @Test
    void obtener_conEscuelaYFoto_devuelveElPerfilCompleto() {
        Usuario usuario = cuenta().fotoUrl("usuarios/u/a.png").build();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenReturn(Optional.of(new EscuelaRef(ESCUELA, "autoescuela", "Autoescuela Sol")));
        when(almacenFotos.urlDeLectura("usuarios/u/a.png")).thenReturn("http://minio/firmada");

        PerfilUsuario perfil = servicio.obtener(usuario.getCodigo());

        assertThat(perfil.codigo()).isEqualTo(usuario.getCodigo());
        assertThat(perfil.nombres()).isEqualTo("Ana");
        assertThat(perfil.apellidos()).isEqualTo("Pérez");
        assertThat(perfil.telefono()).isEqualTo("0991234567");
        assertThat(perfil.fechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(perfil.email()).isEqualTo("ana@gmail.com");
        assertThat(perfil.correoVerificado()).isTrue();
        assertThat(perfil.roles()).containsExactly(RolNombres.ESTUDIANTE);
        assertThat(perfil.escuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(perfil.escuelaNombre()).isEqualTo("Autoescuela Sol");
        assertThat(perfil.fotoUrl()).isEqualTo("http://minio/firmada");
    }

    @Test
    void obtener_sinEscuelaNiFoto_noConsultaNiFirmaNada() {
        Usuario usuario = cuenta().escuelaCodigo(null).build();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        PerfilUsuario perfil = servicio.obtener(usuario.getCodigo());

        assertThat(perfil.escuelaCodigo()).isNull();
        assertThat(perfil.escuelaNombre()).isNull();
        assertThat(perfil.fotoUrl()).isNull();
        verify(escuelaConsulta, never()).buscarPorCodigo(any());
        verify(almacenFotos, never()).urlDeLectura(any());
    }

    @Test
    void obtener_siEscuelaNoResponde_elNombreViajaNuloSinError() {
        Usuario usuario = cuenta().build();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenThrow(new IllegalStateException("Escuela caído"));

        PerfilUsuario perfil = servicio.obtener(usuario.getCodigo());

        assertThat(perfil.escuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(perfil.escuelaNombre()).isNull();
        assertThat(perfil.nombres()).isEqualTo("Ana");
    }

    @Test
    void obtener_siNoSePuedeFirmarLaFoto_laUrlViajaNulaSinError() {
        Usuario usuario = cuenta().escuelaCodigo(null).fotoUrl("usuarios/u/a.png").build();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(almacenFotos.urlDeLectura("usuarios/u/a.png")).thenThrow(new IllegalStateException("MinIO caído"));

        assertThat(servicio.obtener(usuario.getCodigo()).fotoUrl()).isNull();
    }

    @Test
    void obtener_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(codigo))
            .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.SESION_INVALIDA));
    }

    @Test
    void elPerfil_noExponeElHashDeLaContrasenaNiIdsInternos() {
        assertThat(Arrays.stream(PerfilUsuario.class.getRecordComponents()).map(RecordComponent::getName))
            .doesNotContain("passwordHash", "password", "id")
            .containsExactly("codigo", "nombres", "apellidos", "fechaNacimiento", "telefono", "email",
                "correoVerificado", "roles", "escuelaCodigo", "escuelaNombre", "fotoUrl", "ci", "sexo", "direccion",
                "latitud", "longitud", "categoriaSigla");
    }
}
