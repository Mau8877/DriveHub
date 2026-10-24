package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoVehiculoUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private CambiarEstadoVehiculoUseCase useCase;

    private void conEstadoActual(EstadoVehiculo estado) {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(
            Vehiculo.builder().codigo(CODIGO).escuelaCodigo(ESCUELA).placa("ABC-123").marca("Toyota").modelo("Yaris")
                .transmision(Transmision.MANUAL).estado(estado).build()));
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void ejecutar_conTransicionPermitida_guardaElEstadoDestino() {
        conEstadoActual(EstadoVehiculo.DISPONIBLE);
        when(vehiculoRepository.actualizar(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));

        VehiculoOutput resultado = useCase.ejecutar(ESCUELA, CODIGO, EstadoVehiculo.MANTENIMIENTO);

        assertThat(resultado.estado()).isEqualTo(EstadoVehiculo.MANTENIMIENTO);
        verify(vehiculoRepository).actualizar(any());
    }

    @Test
    void ejecutar_conTransicionNoPermitida_lanzaErrEsc09SinGuardar() {
        conEstadoActual(EstadoVehiculo.BAJA);

        esperaCodigo(() -> useCase.ejecutar(ESCUELA, CODIGO, EstadoVehiculo.MANTENIMIENTO),
            EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA);
        verify(vehiculoRepository, never()).actualizar(any());
    }

    @Test
    void ejecutar_conAutoDeBajaADisponible_loReactiva() {
        conEstadoActual(EstadoVehiculo.BAJA);
        when(vehiculoRepository.actualizar(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));

        assertThat(useCase.ejecutar(ESCUELA, CODIGO, EstadoVehiculo.DISPONIBLE).estado())
            .isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void ejecutar_conAutoInexistenteODeOtraEscuela_lanzaErrEsc08() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(ESCUELA, CODIGO, EstadoVehiculo.BAJA),
            EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO);
        verify(vehiculoRepository, never()).actualizar(any());
    }
}
