package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearAdministradorEscuelaComando;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CrearAdministradorEscuelaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final String CLAVE = "claveSecreta123";

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RolRepositoryPort rolRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private CrearAdministradorEscuelaService servicio;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new CrearAdministradorEscuelaService(
            usuarioRepository, rolRepository, passwordHasher, geocodificador, reloj, 18);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(CrearAdministradorEscuelaService.class)).addAppender(logs);
    }

    @AfterEach
    void quitarAppender() {
        ((Logger) LoggerFactory.getLogger(CrearAdministradorEscuelaService.class)).detachAppender(logs);
    }

    private CrearAdministradorEscuelaComando comando(LocalDate nacimiento, String email) {
        return new CrearAdministradorEscuelaComando(" Marta ", "Gómez", nacimiento, email, CLAVE, " ", ESCUELA,
            " mg 98765 ", SexoPersona.FEMENINO, new BigDecimal("-16.4897"), new BigDecimal("-68.1193"));
    }

    private void rolYHashDisponibles() {
        when(rolRepository.buscarPorNombre(RolNombres.ADMIN_ESCUELA)).thenReturn(
            Optional.of(Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.ADMIN_ESCUELA).build()));
        when(passwordHasher.hashear(CLAVE)).thenReturn("$2a$hash");
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crear_conDatosValidos_creaUnAdministradorActivoDeEsaEscuelaSinVerificarSuCorreo() {
        rolYHashDisponibles();

        servicio.crear(comando(LocalDate.of(1985, 5, 5), " Marta@Gmail.com "));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        Usuario usuario = capturado.getValue();
        assertThat(usuario.getEmail()).isEqualTo("marta@gmail.com");
        assertThat(usuario.getNombres()).isEqualTo("Marta");
        assertThat(usuario.getTelefono()).isNull();
        assertThat(usuario.getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.isCorreoVerificado()).isFalse();
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.ADMIN_ESCUELA);
        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$hash").isNotEqualTo(CLAVE);
    }

    @Test
    void crear_conExactamente18AniosHoy_acepta() {
        rolYHashDisponibles();

        servicio.crear(comando(HOY.minusYears(18), "marta@gmail.com"));

        verify(usuarioRepository).guardar(any(Usuario.class));
    }

    @Test
    void crear_conMenosDe18Anios_lanzaErrIde02YNoGuardaNada() {
        assertThatThrownBy(() -> servicio.crear(comando(HOY.minusYears(18).plusDays(1), "marta@gmail.com")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCorreoEnUso_lanzaErrIde01SinTocarLaContrasena() {
        when(usuarioRepository.existePorEmail("marta@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1985, 5, 5), "MARTA@gmail.com")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
        verify(passwordHasher, never()).hashear(any());
    }

    @Test
    void crear_siLaEscuelaYaTieneAdministrador_lanzaErrIde12YNoGuardaNada() {
        when(usuarioRepository.existeAdministradorDeEscuela(ESCUELA)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1985, 5, 5), "marta@gmail.com")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ADMIN_YA_EXISTE_EN_ESCUELA));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_nuncaEscribeLaContrasenaNiSuHashEnLosRegistros() {
        rolYHashDisponibles();

        servicio.crear(comando(LocalDate.of(1985, 5, 5), "marta@gmail.com"));

        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent evento : logs.list) {
            String completo = evento.getFormattedMessage() + Arrays.toString(evento.getArgumentArray());
            assertThat(completo).doesNotContain(CLAVE).doesNotContain("$2a$hash");
        }
    }

    @Test
    void crear_conDatosValidos_creaElPerfilConCiSexoUbicacionYSinCategoria() {
        rolYHashDisponibles();
        when(geocodificador.direccionDe(new BigDecimal("-16.4897"), new BigDecimal("-68.1193")))
            .thenReturn(Optional.of("Plaza Murillo, La Paz"));

        servicio.crear(comando(LocalDate.of(1985, 5, 5), "marta@gmail.com"));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        var perfil = capturado.getValue().getPerfil();
        assertThat(perfil.getCi()).isEqualTo("MG98765");
        assertThat(perfil.getSexo()).isEqualTo(SexoPersona.FEMENINO);
        assertThat(perfil.getDireccion()).isEqualTo("Plaza Murillo, La Paz");
        assertThat(perfil.getCategoriaSigla()).isNull();
    }

    @Test
    void crear_conCiRepetido_lanzaErrIde19YNoGuardaNada() {
        when(usuarioRepository.existePorCi("MG98765")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1985, 5, 5), "marta@gmail.com")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CI_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
    }
}
