package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EditarInstructorInput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
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
class EditarInstructorUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final BigDecimal LAT = new BigDecimal("-17.783300");
    private static final BigDecimal LON = new BigDecimal("-63.182100");

    @Mock
    private InstructorRepository instructores;
    @Mock
    private CategoriaLicenciaRepository categorias;
    @Mock
    private InstructorCuentaGateway cuentas;

    private EditarInstructorUseCase useCase;
    private Instructor actual;

    @BeforeEach
    void crearCasoDeUso() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        useCase = new EditarInstructorUseCase(instructores, categorias, cuentas, new InstructorMapeador(reloj), reloj);
        actual = ficha(HOY.plusYears(1));
        lenient().when(instructores.buscarPorCodigo(ESCUELA, actual.getCodigo())).thenReturn(Optional.of(actual));
    }

    private static Instructor ficha(LocalDate vence) {
        Set<CategoriaLicencia> cats = new LinkedHashSet<>(List.of(CategoriaLicencia.builder().sigla("A").build()));
        return Instructor.builder().codigo(UUID.randomUUID()).usuarioCodigo(USUARIO).escuelaCodigo(ESCUELA)
            .nroLicencia("LIC-1").licenciaVence(vence).estado(EstadoInstructor.ACTIVO)
            .fechaContratacion(LocalDate.of(2026, 1, 1)).categorias(cats)
            .creadoEn(Instant.parse("2026-05-04T15:30:00Z")).build();
    }

    private static PersonaInstructor persona(BigDecimal lat, BigDecimal lon) {
        return new PersonaInstructor(USUARIO, "Ivan", "Pérez", "Ivan Pérez", null, 36, LocalDate.of(1990, 1, 1),
            "MASCULINO", "AB12345", "ivan@gmail.com", null, "Av. Arce", lat, lon, HOY);
    }

    private EditarInstructorInput cambio(String licencia, LocalDate vence, LocalDate contratacion,
            List<String> siglas, EstadoInstructor estado, BigDecimal lat, BigDecimal lon) {
        return new EditarInstructorInput(ESCUELA, actual.getCodigo(), licencia, vence, contratacion, siglas, estado,
            lat, lon);
    }

    private void guardarDevuelveLoQueRecibe() {
        when(instructores.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void assertErrSys01(EditarInstructorInput entrada) {
        assertThatThrownBy(() -> useCase.ejecutar(entrada)).isInstanceOfSatisfying(ValidationException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
    }

    @Test
    void ejecutar_cambiaCadaCampoDeLaFichaYNoTocaLoQueNoLlega() {
        guardarDevuelveLoQueRecibe();
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona(LAT, LON)));

        InstructorDetalleOutput detalle = useCase.ejecutar(
            cambio(null, null, null, null, EstadoInstructor.LICENCIA, null, null));

        ArgumentCaptor<Instructor> guardada = ArgumentCaptor.forClass(Instructor.class);
        verify(instructores).actualizar(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoInstructor.LICENCIA);
        assertThat(guardada.getValue().getNroLicencia()).isEqualTo("LIC-1");
        assertThat(guardada.getValue().getLicenciaVence()).isEqualTo(actual.getLicenciaVence());
        assertThat(guardada.getValue().getUsuarioCodigo()).isEqualTo(USUARIO);
        assertThat(guardada.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(detalle.estado()).isEqualTo(EstadoInstructor.LICENCIA);
        assertThat(detalle.nombres()).isEqualTo("Ivan");
        verify(cuentas, never()).cambiarUbicacion(any(), any(), any());
    }

    @Test
    void ejecutar_conTodosLosCamposJuntos_losAplicaYReemplazaLasCategorias() {
        guardarDevuelveLoQueRecibe();
        when(categorias.buscarPorSiglas(List.of("B", "C"))).thenReturn(List.of(
            CategoriaLicencia.builder().sigla("B").build(), CategoriaLicencia.builder().sigla("C").build()));
        when(instructores.existePorNroLicenciaDeOtro("NUEVA-9", actual.getCodigo())).thenReturn(false);
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona(LAT, LON)));
        when(cuentas.cambiarUbicacion(USUARIO, new BigDecimal("-17.9"), new BigDecimal("-63.3")))
            .thenReturn(persona(new BigDecimal("-17.9"), new BigDecimal("-63.3")));

        InstructorDetalleOutput detalle = useCase.ejecutar(cambio(" nueva-9 ", HOY.plusYears(3), HOY.minusDays(5),
            List.of("b", "c"), EstadoInstructor.INACTIVO, new BigDecimal("-17.9"), new BigDecimal("-63.3")));

        ArgumentCaptor<Instructor> guardada = ArgumentCaptor.forClass(Instructor.class);
        verify(instructores).actualizar(guardada.capture());
        assertThat(guardada.getValue().getNroLicencia()).isEqualTo("NUEVA-9");
        assertThat(guardada.getValue().getLicenciaVence()).isEqualTo(HOY.plusYears(3));
        assertThat(guardada.getValue().getFechaContratacion()).isEqualTo(HOY.minusDays(5));
        assertThat(guardada.getValue().getCategorias()).extracting(CategoriaLicencia::getSigla).containsExactly("B", "C");
        assertThat(detalle.latitud()).isEqualByComparingTo("-17.9");
        assertThat(detalle.categorias()).extracting("sigla").containsExactly("B", "C");
    }

    @Test
    void ejecutar_conInstructorInexistenteODeOtraEscuela_lanzaErrEsc09() {
        UUID otro = UUID.randomUUID();
        when(instructores.buscarPorCodigo(ESCUELA, otro)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(new EditarInstructorInput(ESCUELA, otro, null, null, null, null,
            EstadoInstructor.ACTIVO, null, null))).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
        verifyNoInteractions(cuentas);
        verify(instructores, never()).actualizar(any());
    }

    @Test
    void ejecutar_sinCamposOConUnaSolaCoordenada_lanzaErrSys01SinGuardar() {
        assertErrSys01(cambio(null, null, null, null, null, null, null));
        assertErrSys01(cambio(null, null, null, null, null, LAT, null));
        assertErrSys01(cambio(null, null, null, null, null, null, LON));

        verify(instructores, never()).actualizar(any());
        verifyNoInteractions(cuentas);
    }

    @Test
    void ejecutar_conLicenciaDeOtroInstructor_lanzaErrEsc08YConLaPropiaNoChoca() {
        when(instructores.existePorNroLicenciaDeOtro("OTRA-1", actual.getCodigo())).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(cambio("otra-1", null, null, null, null, null, null)))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
        verify(instructores, never()).actualizar(any());

        guardarDevuelveLoQueRecibe();
        when(cuentas.consultar(any())).thenReturn(List.of());
        useCase.ejecutar(cambio("lic-1", null, null, null, null, null, null));
        verify(instructores, never()).existePorNroLicenciaDeOtro("LIC-1", actual.getCodigo());
    }

    @Test
    void ejecutar_conLicenciaYaVencidaSinTocarla_dejaCorregirOtrosDatos() {
        actual = ficha(HOY.minusMonths(2));
        when(instructores.buscarPorCodigo(ESCUELA, actual.getCodigo())).thenReturn(Optional.of(actual));
        guardarDevuelveLoQueRecibe();
        when(cuentas.consultar(any())).thenReturn(List.of());

        InstructorDetalleOutput detalle = useCase.ejecutar(cambio(null, actual.getLicenciaVence(), null, null,
            EstadoInstructor.LICENCIA, null, null));

        assertThat(detalle.estado()).isEqualTo(EstadoInstructor.LICENCIA);
        assertThat(detalle.licenciaVence()).isEqualTo(HOY.minusMonths(2));
    }

    @Test
    void ejecutar_cambiandoElVencimientoAUnaFechaVencida_lanzaErrSys01() {
        actual = ficha(HOY.minusMonths(2));
        when(instructores.buscarPorCodigo(ESCUELA, actual.getCodigo())).thenReturn(Optional.of(actual));

        assertErrSys01(cambio(null, HOY.minusDays(1), null, null, null, null, null));
        assertErrSys01(cambio(null, HOY, null, null, null, null, null));
        verify(instructores, never()).actualizar(any());
    }

    @Test
    void ejecutar_conContratacionFuturaOCategoriasInvalidas_fallaSinGuardarNiLlamarAIdentidad() {
        assertErrSys01(cambio(null, null, HOY.plusDays(1), null, null, null, null));
        assertErrSys01(cambio(null, null, null, List.of(), null, null, null));
        assertErrSys01(cambio(null, null, null, List.of("A", "a"), null, null, null));

        when(categorias.buscarPorSiglas(List.of("ZZ"))).thenReturn(List.of());
        assertThatThrownBy(() -> useCase.ejecutar(cambio(null, null, null, List.of("zz"), null, null, null)))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.CATEGORIA_INVALIDA));
        when(categorias.buscarPorSiglas(List.of("SIN"))).thenReturn(List.of(
            CategoriaLicencia.builder().sigla("SIN").build()));
        assertThatThrownBy(() -> useCase.ejecutar(cambio(null, null, null, List.of("sin"), null, null, null)))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.CATEGORIA_INVALIDA));

        verify(instructores, never()).actualizar(any());
        verifyNoInteractions(cuentas);
    }

    @Test
    void ejecutar_conElMismoPunto_noLlamaACambiarUbicacion() {
        guardarDevuelveLoQueRecibe();
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona(LAT, LON)));

        useCase.ejecutar(cambio(null, null, null, null, EstadoInstructor.ACTIVO, new BigDecimal("-17.7833"),
            new BigDecimal("-63.1821")));

        verify(cuentas, never()).cambiarUbicacion(any(), any(), any());
    }

    @Test
    void ejecutar_conOtroPunto_cambiaLaUbicacionEnIdentidadAntesDeGuardarLaFicha() {
        guardarDevuelveLoQueRecibe();
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona(LAT, LON)));
        when(cuentas.cambiarUbicacion(any(), any(), any())).thenReturn(persona(BigDecimal.ONE, BigDecimal.TEN));

        useCase.ejecutar(cambio(null, null, null, null, EstadoInstructor.ACTIVO, BigDecimal.ONE, BigDecimal.TEN));

        InOrder orden = inOrder(cuentas, instructores);
        orden.verify(cuentas).cambiarUbicacion(USUARIO, BigDecimal.ONE, BigDecimal.TEN);
        orden.verify(instructores).actualizar(any());
    }

    @Test
    void ejecutar_siIdentidadFallaAlCambiarLaUbicacion_noSeGuardaLaFicha() {
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona(LAT, LON)));
        when(cuentas.cambiarUbicacion(any(), any(), any())).thenThrow(new IdentidadNoDisponibleException("caída"));

        assertThatThrownBy(() -> useCase.ejecutar(cambio(null, null, null, null, EstadoInstructor.ACTIVO,
            BigDecimal.ONE, BigDecimal.TEN))).isInstanceOf(IdentidadNoDisponibleException.class);

        verify(instructores, never()).actualizar(any());
    }
}
