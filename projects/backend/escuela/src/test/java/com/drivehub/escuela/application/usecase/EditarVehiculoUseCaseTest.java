package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EditarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class EditarVehiculoUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private EditarVehiculoUseCase useCase;

    private static Vehiculo vehiculo(EstadoVehiculo estado) {
        return Vehiculo.builder().codigo(CODIGO).escuelaCodigo(ESCUELA).placa("ABC-123").marca("Toyota")
            .modelo("Yaris").anio((short) 2020).color("Rojo").transmision(Transmision.MANUAL).estado(estado)
            .kilometraje(1000).build();
    }

    private static EditarVehiculoInput cambios(String marca, Integer km) {
        return new EditarVehiculoInput(ESCUELA, CODIGO, marca, null, null, null, km, null, null, null);
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void ejecutar_soloCambiaLosCamposEnviadosYDejaPlacaTransmisionYEstado() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(vehiculo(EstadoVehiculo.DISPONIBLE)));
        when(vehiculoRepository.actualizar(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));

        VehiculoOutput resultado = useCase.ejecutar(new EditarVehiculoInput(ESCUELA, CODIGO, " Nissan ", null, null,
            null, 1500, LocalDate.parse("2027-01-01"), null, null));

        ArgumentCaptor<Vehiculo> guardado = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehiculoRepository).actualizar(guardado.capture());
        assertThat(guardado.getValue().getMarca()).isEqualTo("Nissan");
        assertThat(resultado.modelo()).isEqualTo("Yaris");
        assertThat(resultado.color()).isEqualTo("Rojo");
        assertThat(resultado.kilometraje()).isEqualTo(1500);
        assertThat(resultado.soatVence()).isEqualTo(LocalDate.parse("2027-01-01"));
        assertThat(resultado.placa()).isEqualTo("ABC-123");
        assertThat(resultado.transmision()).isEqualTo(Transmision.MANUAL);
        assertThat(resultado.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void ejecutar_conImagenNuevaLaCambiaYSinImagenDejaLaActual() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(
            vehiculo(EstadoVehiculo.DISPONIBLE).toBuilder().imagenUrl("https://res.cloudinary.com/n/vieja.jpg").build()));
        when(vehiculoRepository.actualizar(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));

        assertThat(useCase.ejecutar(cambios("Nissan", null)).imagenUrl()).isEqualTo("https://res.cloudinary.com/n/vieja.jpg");
        assertThat(useCase.ejecutar(new EditarVehiculoInput(ESCUELA, CODIGO, null, null, null, null, null, null, null,
            "https://res.cloudinary.com/n/nueva.jpg")).imagenUrl()).isEqualTo("https://res.cloudinary.com/n/nueva.jpg");
        esperaCodigo(() -> useCase.ejecutar(new EditarVehiculoInput(ESCUELA, CODIGO, null, null, null, null, null, null,
            null, "https://otro-sitio.com/x.jpg")), "ERR_SYS_01");
    }

    @Test
    void ejecutar_conKilometrajeMenorAlActual_lanzaErrSys01() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(vehiculo(EstadoVehiculo.DISPONIBLE)));

        esperaCodigo(() -> useCase.ejecutar(cambios(null, 999)), "ERR_SYS_01");
        verify(vehiculoRepository, never()).actualizar(any());
    }

    @Test
    void ejecutar_conDatosInvalidos_lanzaErrSys01SinTocarElRepositorio() {
        esperaCodigo(() -> useCase.ejecutar(cambios("  ", null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(cambios("T".repeat(41), null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(cambios(null, -5)), "ERR_SYS_01");
        verify(vehiculoRepository, never()).buscarPorCodigo(any(), any());
    }

    @Test
    void ejecutar_conAutoInexistenteODeOtraEscuela_lanzaErrEsc08() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(cambios("Nissan", null)), EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO);
        verify(vehiculoRepository, never()).actualizar(any());
    }

    @Test
    void ejecutar_conAutoDeBaja_lanzaErrEsc09() {
        when(vehiculoRepository.buscarPorCodigo(ESCUELA, CODIGO)).thenReturn(Optional.of(vehiculo(EstadoVehiculo.BAJA)));

        esperaCodigo(() -> useCase.ejecutar(cambios("Nissan", null)), EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA);
        verify(vehiculoRepository, never()).actualizar(any());
    }
}
