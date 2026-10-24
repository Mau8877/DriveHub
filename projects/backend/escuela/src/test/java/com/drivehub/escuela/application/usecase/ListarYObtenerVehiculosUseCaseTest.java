package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.ListarVehiculosInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.FiltroVehiculos;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class ListarYObtenerVehiculosUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private VehiculoRepository vehiculoRepository;

    private static Vehiculo vehiculo() {
        return Vehiculo.builder().codigo(CODIGO).escuelaCodigo(ESCUELA).placa("ABC-123").marca("Toyota")
            .modelo("Yaris").transmision(Transmision.MANUAL).estado(EstadoVehiculo.DISPONIBLE).build();
    }

    @Test
    void listar_sinParametros_usaPagina1De20YCalculaTotales() {
        when(vehiculoRepository.listar(eq(ESCUELA), eq(new FiltroVehiculos(null, null)), eq(1), eq(20)))
            .thenReturn(new Pagina<>(List.of(vehiculo()), 45));

        PaginaOutput<VehiculoOutput> resultado = new ListarVehiculosUseCase(vehiculoRepository)
            .ejecutar(new ListarVehiculosInput(ESCUELA, null, null, null, null));

        assertThat(resultado.elementos()).hasSize(1);
        assertThat(resultado.pagina()).isEqualTo(1);
        assertThat(resultado.tamano()).isEqualTo(20);
        assertThat(resultado.totalElementos()).isEqualTo(45);
        assertThat(resultado.totalPaginas()).isEqualTo(3);
    }

    @Test
    void listar_conTamanoMuyGrande_loLimitaA100YPasaElFiltro() {
        when(vehiculoRepository.listar(eq(ESCUELA), eq(new FiltroVehiculos("yaris", EstadoVehiculo.BAJA)), eq(2),
            eq(100))).thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<VehiculoOutput> resultado = new ListarVehiculosUseCase(vehiculoRepository)
            .ejecutar(new ListarVehiculosInput(ESCUELA, 2, 5000, "yaris", EstadoVehiculo.BAJA));

        ArgumentCaptor<Integer> tamano = ArgumentCaptor.forClass(Integer.class);
        verify(vehiculoRepository).listar(eq(ESCUELA), eq(new FiltroVehiculos("yaris", EstadoVehiculo.BAJA)), eq(2),
            tamano.capture());
        assertThat(tamano.getValue()).isEqualTo(100);
        assertThat(resultado.totalPaginas()).isZero();
    }

    @Test
    void obtener_conAutoDeLaEscuela_loDevuelve() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(vehiculo()));

        assertThat(new ObtenerVehiculoUseCase(vehiculoRepository).ejecutar(ESCUELA, CODIGO).placa())
            .isEqualTo("ABC-123");
    }

    @Test
    void obtener_conAutoInexistenteODeOtraEscuela_lanzaErrEsc08() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ObtenerVehiculoUseCase(vehiculoRepository).ejecutar(ESCUELA, CODIGO))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO));
    }
}
