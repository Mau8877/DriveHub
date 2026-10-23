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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RegistrarEstudianteComando;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RegistrarEstudianteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID ESCUELA_CODIGO = UUID.randomUUID();
    private static final BigDecimal LA_PAZ_LAT = new BigDecimal("-16.489700");
    private static final BigDecimal LA_PAZ_LON = new BigDecimal("-68.119300");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RolRepositoryPort rolRepository;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private RegistrarEstudianteService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new RegistrarEstudianteService(
            usuarioRepository, rolRepository, escuelaConsulta, passwordHasher, geocodificador, reloj, 18);
    }

    private RegistrarEstudianteComando comando(LocalDate nacimiento, String email, String slug) {
        return new RegistrarEstudianteComando(
            " Ana ", "Pérez", nacimiento, email, "123456789", null, slug,
            " ab 12345 ", SexoPersona.FEMENINO, LA_PAZ_LAT, LA_PAZ_LON);
    }

    private void escuelaYRolDisponibles() {
        when(escuelaConsulta.buscarActivaPorSlug("escuelaexample"))
            .thenReturn(Optional.of(new EscuelaRef(ESCUELA_CODIGO, "escuelaexample", "Escuela Example")));
        when(rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE))
            .thenReturn(Optional.of(Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.ESTUDIANTE).build()));
        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(false);
        when(passwordHasher.hashear("123456789")).thenReturn("$2a$hash");
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrar_conDatosValidos_creaEstudianteActivoConSuEscuelaYSinGuardarLaContrasena() {
        escuelaYRolDisponibles();

        servicio.registrar(comando(LocalDate.of(2000, 1, 1), " Ana@Gmail.com ", "escuelaexample"));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        Usuario usuario = capturado.getValue();
        assertThat(usuario.getEmail()).isEqualTo("ana@gmail.com");
        assertThat(usuario.getNombres()).isEqualTo("Ana");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getEscuelaCodigo()).isEqualTo(ESCUELA_CODIGO);
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.ESTUDIANTE);
        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$hash").isNotEqualTo("123456789");
    }

    @Test
    void registrar_conExactamente18AniosHoy_acepta() {
        escuelaYRolDisponibles();

        servicio.registrar(comando(HOY.minusYears(18), "ana@gmail.com", "escuelaexample"));

        verify(usuarioRepository).guardar(any(Usuario.class));
    }

    @Test
    void registrar_conMenosDe18Anios_lanzaErrIde02YNoGuardaNada() {
        LocalDate cumple18Manana = HOY.minusYears(18).plusDays(1);

        assertThatThrownBy(() -> servicio.registrar(comando(cumple18Manana, "ana@gmail.com", "escuelaexample")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void registrar_conEscuelaInexistente_lanzaErrIde03YNoGuardaNada() {
        when(escuelaConsulta.buscarActivaPorSlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(comando(LocalDate.of(2000, 1, 1), "ana@gmail.com", "no-existe")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void registrar_conCorreoRepetidoEnOtrasMayusculas_lanzaErrIde01YNoGuardaNada() {
        when(escuelaConsulta.buscarActivaPorSlug("escuelaexample"))
            .thenReturn(Optional.of(new EscuelaRef(ESCUELA_CODIGO, "escuelaexample", "Escuela Example")));
        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(comando(LocalDate.of(2000, 1, 1), "ANA@Gmail.com", "escuelaexample")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
        verify(passwordHasher, never()).hashear(any());
    }

    @Test
    void registrar_conDatosValidos_creaElPerfilConCiSexoUbicacionYCategoriaSinCategoria() {
        escuelaYRolDisponibles();
        when(geocodificador.direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).thenReturn(Optional.of("Av. Arce 2345, La Paz"));

        servicio.registrar(comando(LocalDate.of(2000, 1, 1), "ana@gmail.com", "escuelaexample"));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        var perfil = capturado.getValue().getPerfil();
        assertThat(perfil.getCi()).isEqualTo("AB12345");
        assertThat(perfil.getSexo()).isEqualTo(SexoPersona.FEMENINO);
        assertThat(perfil.getLatitud()).isEqualByComparingTo(LA_PAZ_LAT);
        assertThat(perfil.getLongitud()).isEqualByComparingTo(LA_PAZ_LON);
        assertThat(perfil.getDireccion()).isEqualTo("Av. Arce 2345, La Paz");
        assertThat(perfil.getCategoriaSigla()).isEqualTo(CategoriaNombres.SIN_CATEGORIA);
    }

    @Test
    void registrar_cuandoElGeocodificadorNoResponde_guardaElPuntoConLaDireccionEnBlanco() {
        escuelaYRolDisponibles();
        when(geocodificador.direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).thenReturn(Optional.empty());

        servicio.registrar(comando(LocalDate.of(2000, 1, 1), "ana@gmail.com", "escuelaexample"));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        assertThat(capturado.getValue().getPerfil().getDireccion()).isNull();
        assertThat(capturado.getValue().getPerfil().getLatitud()).isEqualByComparingTo(LA_PAZ_LAT);
    }

    @Test
    void registrar_conCiRepetido_lanzaErrIde19YNoGuardaNada() {
        when(escuelaConsulta.buscarActivaPorSlug("escuelaexample"))
            .thenReturn(Optional.of(new EscuelaRef(ESCUELA_CODIGO, "escuelaexample", "Escuela Example")));
        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.existePorCi("AB12345")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(comando(LocalDate.of(2000, 1, 1), "ana@gmail.com", "escuelaexample")))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CI_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
        verify(passwordHasher, never()).hashear(any());
        verify(geocodificador, never()).direccionDe(any(), any());
    }
}
