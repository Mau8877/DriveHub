package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.IniciarSesionComando;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class IniciarSesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;
    @Mock
    private EmisorTokenAccesoPort emisorToken;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private AlmacenFotosPort almacenFotos;

    private IniciarSesionService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        SesionEmisora emisora = new SesionEmisora(escuelaConsulta, refreshTokenRepository, emisorToken, generadorToken,
            new PerfilArmador(almacenFotos, escuelaConsulta), 15, 7);
        servicio = new IniciarSesionService(usuarioRepository, refreshTokenRepository, passwordHasher, emisora, reloj, 5, 15);
    }

    private Usuario cuenta(String rol, UUID escuela, EstadoUsuario estado, int intentos, Instant bloqueadoHasta) {
        return Usuario.builder()
            .codigo(UUID.randomUUID()).email("ana@gmail.com").passwordHash("$2a$hash")
            .nombres("Ana").apellidos("Pérez").escuelaCodigo(escuela).estado(estado)
            .intentosFallidos(intentos).bloqueadoHasta(bloqueadoHasta)
            .roles(Set.of(Rol.builder().nombre(rol).build())).build();
    }

    private void cuentaExiste(Usuario usuario) {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.lenient().when(usuarioRepository.actualizar(any(Usuario.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private void passwordCorrecta() {
        when(passwordHasher.coincide("123456789", "$2a$hash")).thenReturn(true);
    }

    private void escuela(boolean activa) {
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenReturn(Optional.of(new EscuelaRef(ESCUELA, "e", "E", activa)));
    }

    private void emisionDisponible() {
        when(generadorToken.generar()).thenReturn("refresh-en-claro");
        when(generadorToken.hashear("refresh-en-claro")).thenReturn("hash-del-refresh");
        when(emisorToken.emitir(any(Usuario.class), any(Instant.class))).thenReturn("jwt");
    }

    private IniciarSesionComando login(Dispositivo dispositivo) {
        return new IniciarSesionComando(" Ana@Gmail.com ", "123456789", dispositivo);
    }

    private void esperaCodigo(IniciarSesionComando comando, String codigo) {
        assertThatThrownBy(() -> servicio.iniciar(comando))
            .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void iniciar_conFoto_devuelveLaUrlFirmada() {
        Usuario conFoto = cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null).toBuilder()
            .fotoUrl("usuarios/u/a.png").build();
        cuentaExiste(conFoto);
        passwordCorrecta();
        escuela(true);
        emisionDisponible();
        when(almacenFotos.urlDeLectura("usuarios/u/a.png")).thenReturn("http://minio/firmada");

        assertThat(servicio.iniciar(login(Dispositivo.WEB)).fotoUrl()).isEqualTo("http://minio/firmada");
    }

    @Test
    void iniciar_sinFoto_laUrlViajaNula() {
        cuentaExiste(cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(true);
        emisionDisponible();

        assertThat(servicio.iniciar(login(Dispositivo.WEB)).fotoUrl()).isNull();
    }

    @Test
    void iniciar_siElAlmacenamientoFalla_iniciaSesionSinLaUrlDeLaFoto() {
        Usuario conFoto = cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null).toBuilder()
            .fotoUrl("usuarios/u/a.png").build();
        cuentaExiste(conFoto);
        passwordCorrecta();
        escuela(true);
        emisionDisponible();
        when(almacenFotos.urlDeLectura("usuarios/u/a.png")).thenThrow(new IllegalStateException("MinIO caído"));

        SesionEmitida sesion = servicio.iniciar(login(Dispositivo.WEB));

        assertThat(sesion.accessToken()).isEqualTo("jwt");
        assertThat(sesion.fotoUrl()).isNull();
    }

    @Test
    void iniciar_adminEnLaWeb_abreSesionRevocaLaPreviaDelDispositivoYActualizaElAcceso() {
        Usuario admin = cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 2, null);
        cuentaExiste(admin);
        passwordCorrecta();
        escuela(true);
        emisionDisponible();

        SesionEmitida sesion = servicio.iniciar(login(Dispositivo.WEB));

        assertThat(sesion.accessToken()).isEqualTo("jwt");
        assertThat(sesion.expiraEnSegundos()).isEqualTo(900);
        assertThat(sesion.refreshToken()).isEqualTo("refresh-en-claro");
        verify(refreshTokenRepository).revocarActivos(admin.getCodigo(), Dispositivo.WEB);
        ArgumentCaptor<RefreshToken> token = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).guardar(token.capture());
        assertThat(token.getValue().getTokenHash()).isEqualTo("hash-del-refresh").isNotEqualTo("refresh-en-claro");
        assertThat(token.getValue().getDispositivo()).isEqualTo(Dispositivo.WEB);
        assertThat(token.getValue().getExpiraEn()).isEqualTo(AHORA.plusSeconds(7 * 24 * 3600));
        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getIntentosFallidos()).isZero();
        assertThat(actualizado.getValue().getUltimoLogin()).isEqualTo(AHORA);
    }

    @Test
    void iniciar_estudianteEnElMovil_abreSesion() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(true);
        emisionDisponible();

        assertThat(servicio.iniciar(login(Dispositivo.MOVIL)).accessToken()).isEqualTo("jwt");
        verify(refreshTokenRepository).revocarActivos(any(UUID.class), org.mockito.ArgumentMatchers.eq(Dispositivo.MOVIL));
    }

    @Test
    void iniciar_superAdminSinEscuela_entraSinConsultarEscuela() {
        cuentaExiste(cuenta(RolNombres.SUPER_ADMIN, null, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        emisionDisponible();

        assertThat(servicio.iniciar(login(Dispositivo.WEB)).accessToken()).isEqualTo("jwt");
        verify(escuelaConsulta, never()).buscarPorCodigo(any());
    }

    @Test
    void iniciar_conCorreoInexistente_lanzaErrIde04SinTocarContadores() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.empty());

        esperaCodigo(login(Dispositivo.WEB), IdentidadErrorCodes.CREDENCIALES_INCORRECTAS);
        verify(usuarioRepository, never()).actualizar(any());
        verify(passwordHasher, never()).coincide(anyString(), anyString());
    }

    @Test
    void iniciar_conContrasenaIncorrecta_sumaUnIntentoYLanzaErrIde04() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.ACTIVO, 3, null));
        when(passwordHasher.coincide("123456789", "$2a$hash")).thenReturn(false);

        esperaCodigo(login(Dispositivo.MOVIL), IdentidadErrorCodes.CREDENCIALES_INCORRECTAS);

        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getIntentosFallidos()).isEqualTo(4);
        assertThat(actualizado.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void iniciar_conElQuintoIntentoFallido_bloqueaLaCuentaPor15MinutosYLanzaErrIde05() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.ACTIVO, 4, null));
        when(passwordHasher.coincide("123456789", "$2a$hash")).thenReturn(false);

        esperaCodigo(login(Dispositivo.MOVIL), IdentidadErrorCodes.CUENTA_BLOQUEADA);

        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
        assertThat(actualizado.getValue().getBloqueadoHasta()).isEqualTo(AHORA.plusSeconds(15 * 60));
    }

    @Test
    void iniciar_conCuentaBloqueadaVigente_lanzaErrIde05AunqueLaContrasenaSeaCorrecta() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.BLOQUEADO, 5, AHORA.plusSeconds(600)));

        esperaCodigo(login(Dispositivo.MOVIL), IdentidadErrorCodes.CUENTA_BLOQUEADA);
        verify(passwordHasher, never()).coincide(anyString(), anyString());
    }

    @Test
    void iniciar_conBloqueoVencido_laDesbloqueaYEntra() {
        Usuario bloqueada = cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.BLOQUEADO, 5, AHORA.minusSeconds(1));
        cuentaExiste(bloqueada);
        passwordCorrecta();
        escuela(true);
        emisionDisponible();

        assertThat(servicio.iniciar(login(Dispositivo.MOVIL)).accessToken()).isEqualTo("jwt");

        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, org.mockito.Mockito.atLeastOnce()).actualizar(actualizado.capture());
        Usuario ultimo = actualizado.getAllValues().get(actualizado.getAllValues().size() - 1);
        assertThat(ultimo.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(ultimo.getIntentosFallidos()).isZero();
        assertThat(ultimo.getBloqueadoHasta()).isNull();
    }

    @Test
    void iniciar_conCuentaInactiva_lanzaErrIde06() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.INACTIVO, 0, null));
        passwordCorrecta();

        esperaCodigo(login(Dispositivo.MOVIL), IdentidadErrorCodes.CUENTA_INACTIVA);
        verify(refreshTokenRepository, never()).guardar(any());
    }

    @Test
    void iniciar_conEscuelaSuspendida_lanzaErrIde07() {
        cuentaExiste(cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(false);

        esperaCodigo(login(Dispositivo.WEB), IdentidadErrorCodes.ESCUELA_SUSPENDIDA);
        verify(refreshTokenRepository, never()).guardar(any());
    }

    @Test
    void iniciar_conEscuelaInactiva_elMensajeDiceQueLaEscuelaEstaInactiva() {
        cuentaExiste(cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(false);

        assertThatThrownBy(() -> servicio.iniciar(login(Dispositivo.WEB)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Tu escuela está inactiva. Contacta al administrador de la plataforma");
    }

    @Test
    void iniciar_conEscuelaInexistente_lanzaErrIde07() {
        cuentaExiste(cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenReturn(Optional.empty());

        esperaCodigo(login(Dispositivo.WEB), IdentidadErrorCodes.ESCUELA_SUSPENDIDA);
    }

    @Test
    void iniciar_conEstudianteEnLaWeb_lanzaErrIde08() {
        cuentaExiste(cuenta(RolNombres.ESTUDIANTE, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(true);

        esperaCodigo(login(Dispositivo.WEB), IdentidadErrorCodes.PLATAFORMA_NO_PERMITIDA);
    }

    @Test
    void iniciar_conAdminEnElMovil_lanzaErrIde08() {
        cuentaExiste(cuenta(RolNombres.ADMIN_ESCUELA, ESCUELA, EstadoUsuario.ACTIVO, 0, null));
        passwordCorrecta();
        escuela(true);

        esperaCodigo(login(Dispositivo.MOVIL), IdentidadErrorCodes.PLATAFORMA_NO_PERMITIDA);
    }
}
