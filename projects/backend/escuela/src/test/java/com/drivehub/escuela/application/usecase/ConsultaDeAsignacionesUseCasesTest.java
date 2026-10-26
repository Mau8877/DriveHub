package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.HistorialAsignacionOutput;
import com.drivehub.escuela.application.dto.ListarAsignacionesInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionHistorial;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

/**
 * CU10: listar autos con su instructor y ver el historial. La autorización por rol NO se prueba acá
 * (un caso de uso sin Spring no ejecuta @PreAuthorize).
 */
@ExtendWith(MockitoExtension.class)
class ConsultaDeAsignacionesUseCasesTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID VEHICULO = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private AsignacionVehiculoRepository asignaciones;
    @Mock
    private VehiculoRepository vehiculos;
    @Mock
    private InstructorCuentaGateway cuentas;

    @InjectMocks
    private ListarAsignacionesVehiculoUseCase listar;
    @InjectMocks
    private ObtenerHistorialVehiculoUseCase historial;

    private static Vehiculo auto(UUID codigo, String placa) {
        return Vehiculo.builder().codigo(codigo).escuelaCodigo(ESCUELA).placa(placa).marca("Toyota").modelo("Yaris")
            .transmision(Transmision.MANUAL).estado(EstadoVehiculo.DISPONIBLE).build();
    }

    private static AsignacionVehiculo vigente() {
        return AsignacionVehiculo.builder().codigo(UUID.randomUUID()).vehiculoCodigo(VEHICULO)
            .instructorCodigo(INSTRUCTOR).fechaInicio(LocalDate.of(2026, 10, 1)).build();
    }

    private static PersonaInstructor persona(String nombre) {
        return new PersonaInstructor(USUARIO, null, null, nombre, null, 30, null, null, null, null, null, null, null,
            null, null);
    }

    private static ListarAsignacionesInput input(String busqueda) {
        return new ListarAsignacionesInput(ESCUELA, null, null, busqueda, null);
    }

    @Test
    void listar_traeAutosConYSinInstructorConElNombreDeIdentidad() {
        UUID libre = UUID.randomUUID();
        when(asignaciones.listar(eq(ESCUELA), any(), eq(1), eq(20))).thenReturn(new Pagina<>(List.of(
            new AsignacionDeAuto(auto(VEHICULO, "ABC-123"), vigente(), USUARIO),
            new AsignacionDeAuto(auto(libre, "XYZ-999"), null, null)), 2));
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona("Ana Pérez")));

        PaginaOutput<AsignacionVehiculoOutput> pagina = listar.ejecutar(input(null));

        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
        assertThat(pagina.elementos().get(0).instructor().nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(pagina.elementos().get(0).instructor().codigo()).isEqualTo(INSTRUCTOR);
        assertThat(pagina.elementos().get(0).fechaInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(pagina.elementos().get(1).instructor()).isNull();
        assertThat(pagina.elementos().get(1).fechaInicio()).isNull();
        verify(cuentas, never()).buscarCodigos(any());
    }

    @Test
    void listar_conBusqueda_sumaLosAutosDeLosInstructoresCuyoNombreCoincide() {
        when(cuentas.buscarCodigos("ana")).thenReturn(List.of(USUARIO));
        when(asignaciones.listar(eq(ESCUELA), any(), eq(1), eq(20))).thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<AsignacionVehiculoOutput> pagina = listar.ejecutar(input("  ana "));

        ArgumentCaptor<FiltroAsignaciones> filtro = ArgumentCaptor.forClass(FiltroAsignaciones.class);
        verify(asignaciones).listar(eq(ESCUELA), filtro.capture(), eq(1), eq(20));
        assertThat(filtro.getValue().busqueda()).isEqualTo("ana");
        assertThat(filtro.getValue().instructorUsuarioCodigos()).containsExactly(USUARIO);
        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalPaginas()).isZero();
        verifyNoInteractions(vehiculos);
    }

    @Test
    void listar_siIdentidadNoDevuelveALaPersona_dejaElNombreEnNullSinRomper() {
        when(asignaciones.listar(eq(ESCUELA), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(
            new AsignacionDeAuto(auto(VEHICULO, "ABC-123"), vigente(), USUARIO)), 1));
        when(cuentas.consultar(any())).thenReturn(List.of());

        PaginaOutput<AsignacionVehiculoOutput> pagina = listar.ejecutar(input(null));

        assertThat(pagina.elementos().get(0).instructor().nombreCompleto()).isNull();
        assertThat(pagina.elementos().get(0).instructor().codigo()).isEqualTo(INSTRUCTOR);
    }

    @Test
    void listar_acotaElTamanoDePagina() {
        when(asignaciones.listar(eq(ESCUELA), any(), eq(3), eq(100))).thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<AsignacionVehiculoOutput> pagina =
            listar.ejecutar(new ListarAsignacionesInput(ESCUELA, 3, 500, null, null));

        assertThat(pagina.tamano()).isEqualTo(100);
        assertThat(pagina.pagina()).isEqualTo(3);
    }

    @Test
    void historial_devuelveLasAsignacionesConElNombreDelInstructor() {
        when(vehiculos.buscarPorCodigo(ESCUELA, VEHICULO)).thenReturn(Optional.of(auto(VEHICULO, "ABC-123")));
        AsignacionVehiculo cerrada = AsignacionVehiculo.builder().codigo(UUID.randomUUID()).vehiculoCodigo(VEHICULO)
            .instructorCodigo(UUID.randomUUID()).fechaInicio(LocalDate.of(2026, 1, 1))
            .fechaFin(LocalDate.of(2026, 9, 30)).build();
        when(asignaciones.historial(ESCUELA, VEHICULO, 1, 20)).thenReturn(new Pagina<>(List.of(
            new AsignacionHistorial(vigente(), USUARIO), new AsignacionHistorial(cerrada, null)), 2));
        when(cuentas.consultar(List.of(USUARIO))).thenReturn(List.of(persona("Ana Pérez")));

        PaginaOutput<HistorialAsignacionOutput> pagina = historial.ejecutar(ESCUELA, VEHICULO, null, null);

        assertThat(pagina.elementos()).hasSize(2);
        assertThat(pagina.elementos().get(0).instructor().nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(pagina.elementos().get(0).fechaFin()).isNull();
        assertThat(pagina.elementos().get(1).fechaFin()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(pagina.elementos().get(1).instructor().nombreCompleto()).isNull();
    }

    @Test
    void historial_conAutoInexistenteODeOtraEscuela_lanzaNotFoundErrEsc09() {
        when(vehiculos.buscarPorCodigo(ESCUELA, VEHICULO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historial.ejecutar(ESCUELA, VEHICULO, 1, 20))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO));
        verifyNoInteractions(asignaciones);
    }
}
