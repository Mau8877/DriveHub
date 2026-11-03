package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.CambiarAsignacionInput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

/**
 * CU10: asignar, cambiar de instructor y liberar. La autorización por rol NO se prueba acá (un caso
 * de uso sin Spring no ejecuta @PreAuthorize): se ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class AsignacionDeVehiculosUseCasesTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID VEHICULO = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID OTRO_INSTRUCTOR = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private AsignacionVehiculoRepository asignaciones;
    @Mock
    private VehiculoRepository vehiculos;
    @Mock
    private InstructorRepository instructores;

    private AsignarVehiculoUseCase asignar;
    private CambiarInstructorVehiculoUseCase cambiar;
    private LiberarVehiculoUseCase liberar;

    @BeforeEach
    void crearCasosDeUso() {
        asignar = new AsignarVehiculoUseCase(asignaciones, vehiculos, instructores, RELOJ);
        cambiar = new CambiarInstructorVehiculoUseCase(asignaciones, vehiculos, instructores, RELOJ);
        liberar = new LiberarVehiculoUseCase(asignaciones, vehiculos, RELOJ);
    }

    private void hayVehiculo(EstadoVehiculo estado) {
        when(vehiculos.buscarPorCodigo(ESCUELA, VEHICULO)).thenReturn(Optional.of(
            Vehiculo.builder().codigo(VEHICULO).escuelaCodigo(ESCUELA).placa("ABC-123").marca("Toyota")
                .modelo("Yaris").transmision(Transmision.MANUAL).estado(estado).build()));
    }

    private void hayInstructor(UUID codigo, EstadoInstructor estado) {
        when(instructores.buscarPorCodigo(ESCUELA, codigo)).thenReturn(Optional.of(
            Instructor.builder().codigo(codigo).usuarioCodigo(UUID.randomUUID()).escuelaCodigo(ESCUELA)
                .nroLicencia("LIC-1").estado(estado).build()));
    }

    private void hayVigente(UUID instructor, LocalDate desde) {
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.of(AsignacionVehiculo.builder()
            .codigo(UUID.randomUUID()).vehiculoCodigo(VEHICULO).instructorCodigo(instructor).fechaInicio(desde)
            .build()));
    }

    private static CambiarAsignacionInput entrada(UUID instructor, LocalDate fecha) {
        return new CambiarAsignacionInput(ESCUELA, VEHICULO, instructor, fecha);
    }

    private static void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    // --- asignar

    @Test
    void asignar_conAutoDisponibleEInstructorActivo_abreLaAsignacionConHoyPorDefecto() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(INSTRUCTOR, EstadoInstructor.ACTIVO);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        when(asignaciones.ultimaFechaFin(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        when(asignaciones.abrir(eq(ESCUELA), any())).thenAnswer(i -> i.getArgument(1));

        AsignacionVehiculoOutput salida = asignar.ejecutar(entrada(INSTRUCTOR, null));

        ArgumentCaptor<AsignacionVehiculo> abierta = ArgumentCaptor.forClass(AsignacionVehiculo.class);
        verify(asignaciones).abrir(eq(ESCUELA), abierta.capture());
        assertThat(abierta.getValue().getFechaInicio()).isEqualTo(HOY);
        assertThat(abierta.getValue().getInstructorCodigo()).isEqualTo(INSTRUCTOR);
        assertThat(abierta.getValue().getFechaFin()).isNull();
        assertThat(salida.instructor().codigo()).isEqualTo(INSTRUCTOR);
        assertThat(salida.instructor().nombreCompleto()).isNull();
        assertThat(salida.estadoVehiculo()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void asignar_conAutoOInstructorInexistente_lanzaNotFound() {
        when(vehiculos.buscarPorCodigo(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO);

        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        when(instructores.buscarPorCodigo(ESCUELA, INSTRUCTOR)).thenReturn(Optional.empty());
        esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO);
        verify(asignaciones, never()).abrir(any(), any());
    }

    @Test
    void asignar_conAutoQueYaTieneInstructor_lanzaConflictErrEsc16() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVigente(OTRO_INSTRUCTOR, HOY.minusDays(3));

        esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_YA_ASIGNADO);
        verify(asignaciones, never()).abrir(any(), any());
    }

    @Test
    void asignar_conAutoNoDisponible_lanzaConflictErrEsc17() {
        hayInstructor(INSTRUCTOR, EstadoInstructor.ACTIVO);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        for (EstadoVehiculo estado : new EstadoVehiculo[] {EstadoVehiculo.MANTENIMIENTO, EstadoVehiculo.BAJA}) {
            hayVehiculo(estado);
            esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_NO_DISPONIBLE);
        }
        verify(asignaciones, never()).abrir(any(), any());
    }

    @Test
    void asignar_conInstructorDeLicenciaOInactivo_lanzaConflictErrEsc18() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        for (EstadoInstructor estado : new EstadoInstructor[] {EstadoInstructor.LICENCIA, EstadoInstructor.INACTIVO}) {
            hayInstructor(INSTRUCTOR, estado);
            esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.INSTRUCTOR_NO_ACTIVO);
        }
        verify(asignaciones, never()).abrir(any(), any());
    }

    @Test
    void asignar_conFechaFuturaOAnteriorAlFinDeLaUltimaAsignacion_lanzaValidationErrSys01() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(INSTRUCTOR, EstadoInstructor.ACTIVO);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());
        when(asignaciones.ultimaFechaFin(ESCUELA, VEHICULO)).thenReturn(Optional.of(HOY.minusDays(2)));

        esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, HOY.plusDays(1))), EscuelaErrorCodes.VALIDACION);
        esperaCodigo(() -> asignar.ejecutar(entrada(INSTRUCTOR, HOY.minusDays(5))), EscuelaErrorCodes.VALIDACION);
        verify(asignaciones, never()).abrir(any(), any());
    }

    // --- cambiar de instructor

    @Test
    void cambiar_conOtroInstructorActivo_cierraYAbreJuntasConLaFecha() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVigente(INSTRUCTOR, HOY.minusDays(10));
        when(asignaciones.cerrarYAbrir(eq(ESCUELA), eq(VEHICULO), eq(HOY.minusDays(1)), any()))
            .thenAnswer(i -> i.getArgument(3));

        AsignacionVehiculoOutput salida = cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, HOY.minusDays(1)));

        ArgumentCaptor<AsignacionVehiculo> nueva = ArgumentCaptor.forClass(AsignacionVehiculo.class);
        verify(asignaciones).cerrarYAbrir(eq(ESCUELA), eq(VEHICULO), eq(HOY.minusDays(1)), nueva.capture());
        assertThat(nueva.getValue().getInstructorCodigo()).isEqualTo(OTRO_INSTRUCTOR);
        assertThat(nueva.getValue().getFechaInicio()).isEqualTo(HOY.minusDays(1));
        assertThat(salida.instructor().codigo()).isEqualTo(OTRO_INSTRUCTOR);
    }

    @Test
    void cambiar_sinAsignacionVigente_lanzaConflictErrEsc19() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.ACTIVO);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());

        esperaCodigo(() -> cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_SIN_ASIGNACION);
    }

    @Test
    void cambiar_alMismoInstructor_lanzaConflictErrEsc16() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVigente(INSTRUCTOR, HOY.minusDays(1));

        esperaCodigo(() -> cambiar.ejecutar(entrada(INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_YA_ASIGNADO);
        verify(asignaciones, never()).cerrarYAbrir(any(), any(), any(), any());
    }

    @Test
    void cambiar_conAutoNoDisponibleOInstructorNoActivo_lanzaErrEsc17OErrEsc18() {
        hayVigente(INSTRUCTOR, HOY.minusDays(1));
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVehiculo(EstadoVehiculo.MANTENIMIENTO);
        esperaCodigo(() -> cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_NO_DISPONIBLE);

        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.LICENCIA);
        esperaCodigo(() -> cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, null)), EscuelaErrorCodes.INSTRUCTOR_NO_ACTIVO);
        verify(asignaciones, never()).cerrarYAbrir(any(), any(), any(), any());
    }

    @Test
    void cambiar_conFechaAnteriorAlInicioDeLaVigente_lanzaValidationErrSys01() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVigente(INSTRUCTOR, HOY.minusDays(2));

        esperaCodigo(() -> cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, HOY.minusDays(3))), EscuelaErrorCodes.VALIDACION);
        verify(asignaciones, never()).cerrarYAbrir(any(), any(), any(), any());
    }

    @Test
    void cambiar_siFallaElCierreOLaApertura_propagaElErrorYNoHaceMasLlamadas() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayInstructor(OTRO_INSTRUCTOR, EstadoInstructor.ACTIVO);
        hayVigente(INSTRUCTOR, HOY.minusDays(2));
        when(asignaciones.cerrarYAbrir(any(), any(), any(), any())).thenThrow(
            new ConflictException(EscuelaErrorCodes.VEHICULO_YA_ASIGNADO, "choque"));

        esperaCodigo(() -> cambiar.ejecutar(entrada(OTRO_INSTRUCTOR, null)), EscuelaErrorCodes.VEHICULO_YA_ASIGNADO);
        verify(asignaciones, never()).cerrar(any(), any(), any());
        verify(asignaciones, never()).abrir(any(), any());
    }

    // --- liberar

    @Test
    void liberar_conAsignacionVigente_laCierraConLaFechaYDejaElAutoSinInstructor() {
        hayVehiculo(EstadoVehiculo.BAJA);
        hayVigente(INSTRUCTOR, HOY.minusDays(4));

        AsignacionVehiculoOutput salida = liberar.ejecutar(ESCUELA, VEHICULO, null);

        verify(asignaciones).cerrar(ESCUELA, VEHICULO, HOY);
        assertThat(salida.instructor()).isNull();
        assertThat(salida.fechaInicio()).isNull();
        assertThat(salida.estadoVehiculo()).isEqualTo(EstadoVehiculo.BAJA);
    }

    @Test
    void liberar_sinAsignacionVigente_lanzaConflictErrEsc19() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        when(asignaciones.buscarVigente(ESCUELA, VEHICULO)).thenReturn(Optional.empty());

        esperaCodigo(() -> liberar.ejecutar(ESCUELA, VEHICULO, null), EscuelaErrorCodes.VEHICULO_SIN_ASIGNACION);
        verify(asignaciones, never()).cerrar(any(), any(), any());
    }

    @Test
    void liberar_conFechaFuturaOAnteriorAlInicio_lanzaValidationErrSys01() {
        hayVehiculo(EstadoVehiculo.DISPONIBLE);
        hayVigente(INSTRUCTOR, HOY.minusDays(2));

        esperaCodigo(() -> liberar.ejecutar(ESCUELA, VEHICULO, HOY.plusDays(1)), EscuelaErrorCodes.VALIDACION);
        esperaCodigo(() -> liberar.ejecutar(ESCUELA, VEHICULO, HOY.minusDays(3)), EscuelaErrorCodes.VALIDACION);
        verify(asignaciones, never()).cerrar(any(), any(), any());
    }

    @Test
    void liberar_conAutoInexistenteODeOtraEscuela_lanzaNotFoundErrEsc09() {
        when(vehiculos.buscarPorCodigo(ESCUELA, VEHICULO)).thenReturn(Optional.empty());

        esperaCodigo(() -> liberar.ejecutar(ESCUELA, VEHICULO, null), EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO);
    }
}
