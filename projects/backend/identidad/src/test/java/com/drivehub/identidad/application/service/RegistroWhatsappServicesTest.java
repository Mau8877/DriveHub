package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.SolicitudRegistroWhatsapp;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.model.VinculoWhatsapp;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappComando;
import com.drivehub.identidad.domain.port.in.DatoRegistro;
import com.drivehub.identidad.domain.port.in.EnlaceRegistro;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.SolicitudRegistroWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

/** CU48: crear la solicitud, completar el registro con la contraseña y validar un dato. */
@ExtendWith(MockitoExtension.class)
class RegistroWhatsappServicesTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private AltaCuenta altaCuenta;
    @Mock
    private SolicitudRegistroWhatsappRepositoryPort solicitudRepository;
    @Mock
    private VinculoWhatsappRepositoryPort vinculoRepository;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private HasheadorWhatsappPort hasheador;

    private CrearSolicitudRegistroWhatsappService crear;
    private CompletarRegistroWhatsappService completar;

    @BeforeEach
    void crearServicios() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        crear = new CrearSolicitudRegistroWhatsappService(altaCuenta, solicitudRepository, generadorToken, reloj,
            "http://localhost:4200/", 30);
        completar = new CompletarRegistroWhatsappService(altaCuenta, solicitudRepository, vinculoRepository,
            escuelaConsulta, generadorToken, hasheador, reloj, ESCUELA);
    }

    private CrearSolicitudRegistroWhatsappComando comando() {
        return new CrearSolicitudRegistroWhatsappComando("+591 7123-4567", " Ana ", " Pérez ",
            LocalDate.of(2000, 1, 1), SexoPersona.FEMENINO, "ab 123", " Ana@Gmail.com ", new BigDecimal("-17.78"),
            new BigDecimal("-63.18"));
    }

    private SolicitudRegistroWhatsapp solicitud(Instant expira) {
        return SolicitudRegistroWhatsapp.builder().codigo(UUID.randomUUID()).telefono("59171234567").nombres("Ana")
            .apellidos("Pérez").fechaNacimiento(LocalDate.of(2000, 1, 1)).sexo(SexoPersona.FEMENINO).ci("AB123")
            .correo("ana@gmail.com").latitud(new BigDecimal("-17.78")).longitud(new BigDecimal("-63.18"))
            .tokenHash("hash-token").expiraEn(expira).build();
    }

    // ---- crear la solicitud

    @Test
    void crear_conDatosValidos_guardaLaSolicitudYDevuelveElEnlaceConVencimientoDe30Minutos() {
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-token");
        when(solicitudRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        EnlaceRegistro enlace = crear.crear(comando());

        assertThat(enlace.enlace()).isEqualTo("http://localhost:4200/account/set-password?token=token-en-claro");
        assertThat(enlace.expiraEn()).isEqualTo(AHORA.plusSeconds(1800));
        ArgumentCaptor<SolicitudRegistroWhatsapp> guardada = ArgumentCaptor.forClass(SolicitudRegistroWhatsapp.class);
        InOrder orden = inOrder(solicitudRepository);
        orden.verify(solicitudRepository).eliminarDelTelefono("59171234567");
        orden.verify(solicitudRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getTokenHash()).isEqualTo("hash-token");
        assertThat(guardada.getValue().getCorreo()).isEqualTo("ana@gmail.com");
        assertThat(guardada.getValue().getCi()).isEqualTo("AB123");
        assertThat(guardada.getValue().getNombres()).isEqualTo("Ana");
        verify(altaCuenta).verificarEdadMinima(LocalDate.of(2000, 1, 1));
        verify(altaCuenta).verificarCorreoLibre("ana@gmail.com");
        verify(altaCuenta).verificarCiLibre("AB123");
    }

    @Test
    void crear_conCorreoYaRegistrado_noGuardaNada() {
        doThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "en uso"))
            .when(altaCuenta).verificarCorreoLibre("ana@gmail.com");

        assertThatThrownBy(() -> crear.crear(comando())).isInstanceOf(ConflictException.class);

        verify(solicitudRepository, never()).guardar(any());
    }

    // ---- completar el registro

    @Test
    void completar_conEnlaceValido_creaLaCuentaVinculaElNumeroYBorraLaSolicitud() {
        SolicitudRegistroWhatsapp s = solicitud(AHORA.plusSeconds(60));
        Usuario cuenta = Usuario.builder().codigo(UUID.randomUUID()).build();
        when(generadorToken.hashear("token")).thenReturn("hash-token");
        when(solicitudRepository.buscarPorTokenHash("hash-token")).thenReturn(Optional.of(s));
        when(altaCuenta.crear(any(), any(), anyString(), anyString())).thenAnswer(i -> {
            assertThat(i.<java.util.function.Supplier<UUID>>getArgument(1).get()).isEqualTo(ESCUELA);
            return cuenta;
        });
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenReturn(Optional.of(new EscuelaRef(ESCUELA, "ejemplo", "Ejemplo")));
        when(hasheador.hashearTelefono("59171234567")).thenReturn("tel-hash");
        when(vinculoRepository.buscarPorTelefonoHash("tel-hash")).thenReturn(Optional.empty());

        boolean vinculado = completar.completar("token", "123456789");

        assertThat(vinculado).isTrue();
        ArgumentCaptor<AltaCuenta.Datos> datos = ArgumentCaptor.forClass(AltaCuenta.Datos.class);
        verify(altaCuenta).crear(datos.capture(), any(), org.mockito.ArgumentMatchers.eq("ESTUDIANTE"),
            org.mockito.ArgumentMatchers.eq("SIN"));
        assertThat(datos.getValue().password()).isEqualTo("123456789");
        assertThat(datos.getValue().telefono()).isEqualTo("+59171234567");
        assertThat(datos.getValue().email()).isEqualTo("ana@gmail.com");
        verify(vinculoRepository).reemplazar(cuenta.getCodigo(), "tel-hash");
        verify(solicitudRepository).eliminar(s);
    }

    @Test
    void completar_conNumeroYaVinculadoAOtraCuenta_creaLaCuentaPeroNoRobaElNumero() {
        SolicitudRegistroWhatsapp s = solicitud(AHORA.plusSeconds(60));
        when(generadorToken.hashear("token")).thenReturn("hash-token");
        when(solicitudRepository.buscarPorTokenHash("hash-token")).thenReturn(Optional.of(s));
        when(altaCuenta.crear(any(), any(), anyString(), anyString()))
            .thenReturn(Usuario.builder().codigo(UUID.randomUUID()).build());
        when(hasheador.hashearTelefono("59171234567")).thenReturn("tel-hash");
        when(vinculoRepository.buscarPorTelefonoHash("tel-hash")).thenReturn(Optional.of(
            VinculoWhatsapp.builder().usuarioCodigo(UUID.randomUUID()).telefonoHash("tel-hash").build()));

        boolean vinculado = completar.completar("token", "123456789");

        assertThat(vinculado).isFalse();
        verify(vinculoRepository, never()).reemplazar(any(), any());
        verify(solicitudRepository).eliminar(s);
    }

    @Test
    void completar_conEnlaceVencido_lanzaErr28SinCrearNada() {
        when(generadorToken.hashear("token")).thenReturn("hash-token");
        when(solicitudRepository.buscarPorTokenHash("hash-token"))
            .thenReturn(Optional.of(solicitud(AHORA.minusSeconds(1))));

        assertThatThrownBy(() -> completar.completar("token", "123456789"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.ENLACE_REGISTRO_INVALIDO));

        verify(altaCuenta, never()).crear(any(), any(), anyString(), anyString());
    }

    @Test
    void completar_conEnlaceInexistente_lanzaErr28() {
        when(generadorToken.hashear("token")).thenReturn("hash-token");
        when(solicitudRepository.buscarPorTokenHash("hash-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> completar.completar("token", "123456789")).isInstanceOf(BusinessException.class);
    }

    @Test
    void completar_siElCorreoSeOcupoEntretanto_dejaPasarElConflictoYNoBorraNada() {
        when(generadorToken.hashear("token")).thenReturn("hash-token");
        when(solicitudRepository.buscarPorTokenHash("hash-token"))
            .thenReturn(Optional.of(solicitud(AHORA.plusSeconds(60))));
        when(altaCuenta.crear(any(), any(), anyString(), anyString()))
            .thenThrow(new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "en uso"));

        assertThatThrownBy(() -> completar.completar("token", "123456789")).isInstanceOf(ConflictException.class);

        verify(solicitudRepository, never()).eliminar(any());
        verify(vinculoRepository, never()).reemplazar(any(), any());
    }

    // ---- validar un dato

    @Test
    void validar_delegaCadaDatoEnLasReglasDeAltaCuenta() {
        ValidarDatoRegistroService servicio = new ValidarDatoRegistroService(altaCuenta);

        servicio.validar(new DatoRegistro(DatoRegistro.Tipo.CORREO, " Ana@Gmail.com "));
        servicio.validar(new DatoRegistro(DatoRegistro.Tipo.CI, "ab 123"));
        servicio.validar(new DatoRegistro(DatoRegistro.Tipo.FECHA_NACIMIENTO, "2000-01-01"));

        verify(altaCuenta).verificarCorreoLibre("ana@gmail.com");
        verify(altaCuenta).verificarCiLibre("AB123");
        verify(altaCuenta).verificarEdadMinima(LocalDate.of(2000, 1, 1));
    }
}
