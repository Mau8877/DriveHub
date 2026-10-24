package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.escuela.application.dto.CrearInstructorInput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.gateway.DatosCuentaInstructor;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

/**
 * La autorización por rol NO se prueba acá (un caso de uso sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class CrearInstructorUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final String CLAVE = "claveSecreta123";

    @Mock
    private InstructorRepository instructores;
    @Mock
    private CategoriaLicenciaRepository categorias;
    @Mock
    private InstructorCuentaGateway cuentas;

    private PlatformTransactionManager transacciones;
    private CrearInstructorUseCase useCase;

    @BeforeEach
    void crearCasoDeUso() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        transacciones = mock(PlatformTransactionManager.class);
        lenient().when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new CrearInstructorUseCase(instructores, categorias, cuentas, new InstructorMapeador(reloj),
            new TransactionTemplate(transacciones), reloj);
    }

    private static CrearInstructorInput entrada(String licencia, LocalDate vence, LocalDate contratacion,
            List<String> siglas) {
        return new CrearInstructorInput(ESCUELA, "Ivan", "Pérez", LocalDate.of(1990, 1, 1), "MASCULINO", "ab12345",
            null, "ivan@gmail.com", CLAVE, new BigDecimal("-17.7833"), new BigDecimal("-63.1821"), licencia, vence,
            contratacion, siglas);
    }

    private static CrearInstructorInput valida() {
        return entrada(" lic 1 ", HOY.plusYears(2), null, List.of("a", "b"));
    }

    private static List<CategoriaLicencia> catalogo(String... siglas) {
        return java.util.Arrays.stream(siglas).map(s -> CategoriaLicencia.builder().sigla(s)
            .descripcion("Cat " + s).build()).toList();
    }

    private static PersonaInstructor personaDe(DatosCuentaInstructor datos) {
        return new PersonaInstructor(datos.usuarioCodigo(), datos.nombres(), datos.apellidos(),
            datos.nombres() + " " + datos.apellidos(), null, 36, datos.fechaNacimiento(), datos.sexo(), datos.ci(),
            datos.email(), datos.telefono(), "Av. Arce", datos.latitud(), datos.longitud(), HOY);
    }

    private void todoDisponible() {
        when(instructores.existePorNroLicencia("LIC1")).thenReturn(false);
        when(categorias.buscarPorSiglas(List.of("A", "B"))).thenReturn(catalogo("A", "B"));
        when(cuentas.crearCuenta(any())).thenAnswer(inv -> personaDe(inv.getArgument(0)));
        when(instructores.guardar(any())).thenAnswer(inv ->
            ((Instructor) inv.getArgument(0)).toBuilder().creadoEn(Instant.parse("2026-10-23T12:00:00Z")).build());
    }

    @Test
    void ejecutar_conDatosValidos_validaCreaLaCuentaYGuardaLaFichaEnEseOrden() {
        todoDisponible();

        InstructorDetalleOutput detalle = useCase.ejecutar(valida());

        InOrder orden = inOrder(instructores, categorias, cuentas, transacciones);
        orden.verify(instructores).existePorNroLicencia("LIC1");
        orden.verify(categorias).buscarPorSiglas(List.of("A", "B"));
        orden.verify(cuentas).crearCuenta(any());
        orden.verify(transacciones).getTransaction(any());
        orden.verify(instructores).guardar(any());
        orden.verify(transacciones).commit(any());
        verify(cuentas, never()).revertirCuenta(any());

        assertThat(detalle.nroLicencia()).isEqualTo("LIC1");
        assertThat(detalle.estado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(detalle.nombreCompleto()).isEqualTo("Ivan Pérez");
        assertThat(detalle.categorias()).extracting("sigla").containsExactly("A", "B");
        assertThat(detalle.fechaContratacion()).isEqualTo(HOY);
        assertThat(detalle.fechaAlta()).isEqualTo(HOY);
    }

    @Test
    void ejecutar_laFichaGuardadaTieneElMismoUsuarioCodigoQueSeMandoAIdentidadYLaEscuelaDelInput() {
        todoDisponible();

        useCase.ejecutar(valida());

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        ArgumentCaptor<Instructor> ficha = ArgumentCaptor.forClass(Instructor.class);
        verify(cuentas).crearCuenta(datos.capture());
        verify(instructores).guardar(ficha.capture());
        assertThat(ficha.getValue().getUsuarioCodigo()).isEqualTo(datos.getValue().usuarioCodigo());
        assertThat(ficha.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(ficha.getValue().getEstado()).isEqualTo(EstadoInstructor.ACTIVO);
        assertThat(datos.getValue().password()).isEqualTo(CLAVE);
    }

    @Test
    void ejecutar_conReglasDeFichaIncumplidas_lanzaErrSys01SinLlamarAIdentidad() {
        assertErrSys01(entrada("LIC1", HOY, null, List.of("A")));
        assertErrSys01(entrada("LIC1", HOY.minusDays(1), null, List.of("A")));
        assertErrSys01(entrada("LIC1", HOY.plusDays(1), HOY.plusDays(1), List.of("A")));
        assertErrSys01(entrada("LIC1", HOY.plusDays(1), null, List.of()));
        assertErrSys01(entrada("LIC1", HOY.plusDays(1), null, List.of("A", "a")));
        assertErrSys01(entrada("AB", HOY.plusDays(1), null, List.of("A")));

        verifyNoInteractions(cuentas);
        verify(instructores, never()).guardar(any());
    }

    private void assertErrSys01(CrearInstructorInput entrada) {
        assertThatThrownBy(() -> useCase.ejecutar(entrada)).isInstanceOfSatisfying(ValidationException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
    }

    @Test
    void ejecutar_conLicenciaEnUso_lanzaErrEsc08SinLlamarAIdentidad() {
        when(instructores.existePorNroLicencia("LIC1")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(valida())).isInstanceOfSatisfying(ConflictException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
        verifyNoInteractions(cuentas, categorias);
    }

    @Test
    void ejecutar_conCategoriaInexistenteOSin_lanzaErrEsc10SinLlamarAIdentidad() {
        when(categorias.buscarPorSiglas(List.of("A", "ZZ"))).thenReturn(catalogo("A"));
        assertThatThrownBy(() -> useCase.ejecutar(entrada("LIC1", HOY.plusDays(1), null, List.of("A", "ZZ"))))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.CATEGORIA_INVALIDA));

        when(categorias.buscarPorSiglas(List.of("A", "SIN"))).thenReturn(catalogo("A", "SIN"));
        assertThatThrownBy(() -> useCase.ejecutar(entrada("LIC1", HOY.plusDays(1), null, List.of("A", "SIN"))))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.CATEGORIA_INVALIDA));

        verifyNoInteractions(cuentas);
    }

    @Test
    void ejecutar_siIdentidadRechaza_laExcepcionSubeTalCualYNoSeGuardaNiSeRevierte() {
        when(categorias.buscarPorSiglas(any())).thenReturn(catalogo("A", "B"));
        when(cuentas.crearCuenta(any()))
            .thenThrow(new ConflictException("ERR_IDE_01", "Ya existe una cuenta con ese correo"));

        assertThatThrownBy(() -> useCase.ejecutar(valida())).isInstanceOfSatisfying(ConflictException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_IDE_01"));
        verify(instructores, never()).guardar(any());
        verify(cuentas, never()).revertirCuenta(any());
    }

    @Test
    void ejecutar_siIdentidadNoResponde_revierteLaCuentaConElMismoCodigoQueSeMando() {
        when(categorias.buscarPorSiglas(any())).thenReturn(catalogo("A", "B"));
        when(cuentas.crearCuenta(any())).thenThrow(new IdentidadNoDisponibleException("caída"));

        assertThatThrownBy(() -> useCase.ejecutar(valida())).isInstanceOf(IdentidadNoDisponibleException.class);

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        ArgumentCaptor<UUID> revertido = ArgumentCaptor.forClass(UUID.class);
        verify(cuentas).crearCuenta(datos.capture());
        verify(cuentas).revertirCuenta(revertido.capture());
        assertThat(revertido.getValue()).isEqualTo(datos.getValue().usuarioCodigo());
        verify(instructores, never()).guardar(any());
    }

    @Test
    void ejecutar_siFallaGuardarLaFicha_revierteLaCuentaYRelanzaElErrorOriginal() {
        when(categorias.buscarPorSiglas(any())).thenReturn(catalogo("A", "B"));
        when(cuentas.crearCuenta(any())).thenAnswer(inv -> personaDe(inv.getArgument(0)));
        when(instructores.guardar(any())).thenThrow(
            new ConflictException(EscuelaErrorCodes.NRO_LICENCIA_EN_USO, "El número de licencia ya está en uso"));

        assertThatThrownBy(() -> useCase.ejecutar(valida())).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.NRO_LICENCIA_EN_USO));

        ArgumentCaptor<DatosCuentaInstructor> datos = ArgumentCaptor.forClass(DatosCuentaInstructor.class);
        verify(cuentas).crearCuenta(datos.capture());
        verify(cuentas).revertirCuenta(datos.getValue().usuarioCodigo());
        verify(transacciones).rollback(any());
    }

    @Test
    void ejecutar_siLaCompensacionFalla_elErrorOriginalEsElQueSale() {
        when(categorias.buscarPorSiglas(any())).thenReturn(catalogo("A", "B"));
        when(cuentas.crearCuenta(any())).thenAnswer(inv -> personaDe(inv.getArgument(0)));
        when(instructores.guardar(any())).thenThrow(new IllegalStateException("base caída"));
        doThrow(new IdentidadNoDisponibleException("sin red")).when(cuentas).revertirCuenta(any());

        assertThatThrownBy(() -> useCase.ejecutar(valida()))
            .isInstanceOf(IllegalStateException.class).hasMessage("base caída");
    }

    @Test
    void ejecutar_lasExcepcionesYElInputNoContienenLaContrasenaNiDatosSensibles() {
        CrearInstructorInput input = valida();

        assertThat(input.toString()).doesNotContain(CLAVE, "ab12345", "ivan@gmail.com", "LIC");
    }
}
