package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.RegistrarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class RegistrarVehiculoUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private RegistrarVehiculoUseCase useCase;

    private static RegistrarVehiculoInput input(String placa, String marca, Short anio, Integer km) {
        return new RegistrarVehiculoInput(ESCUELA, placa, marca, "Yaris", anio, " Rojo ", Transmision.MANUAL, km, null, null, null);
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void ejecutar_conDatosValidos_guardaDisponibleDeLaEscuelaConPlacaEnMayusculas() {
        when(vehiculoRepository.existePorPlaca("ABC-123")).thenReturn(false);
        when(vehiculoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

        VehiculoOutput resultado = useCase.ejecutar(input(" abc-123 ", " Toyota ", (short) 2020, null));

        ArgumentCaptor<Vehiculo> guardado = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehiculoRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getCodigo()).isNotNull();
        assertThat(guardado.getValue().getColor()).isEqualTo("Rojo");
        assertThat(resultado.placa()).isEqualTo("ABC-123");
        assertThat(resultado.marca()).isEqualTo("Toyota");
        assertThat(resultado.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(resultado.kilometraje()).isZero();
    }

    @Test
    void ejecutar_conImagenDeCloudinary_laGuardaYConImagenAjena_lanzaErrSys01() {
        when(vehiculoRepository.existePorPlaca("ABC-123")).thenReturn(false);
        when(vehiculoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        String foto = "https://res.cloudinary.com/nube/image/upload/v1/auto.jpg";

        VehiculoOutput resultado = useCase.ejecutar(new RegistrarVehiculoInput(ESCUELA, "ABC-123", "Toyota", "Yaris",
            null, null, Transmision.MANUAL, null, null, null, foto));

        assertThat(resultado.imagenUrl()).isEqualTo(foto);
        esperaCodigo(() -> useCase.ejecutar(new RegistrarVehiculoInput(ESCUELA, "ABC-124", "Toyota", "Yaris", null,
            null, Transmision.MANUAL, null, null, null, "https://sitio-ajeno.com/auto.jpg")), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(new RegistrarVehiculoInput(ESCUELA, "ABC-124", "Toyota", "Yaris", null,
            null, Transmision.MANUAL, null, null, null, "http://res.cloudinary.com/nube/auto.jpg")), "ERR_SYS_01");
    }

    @Test
    void ejecutar_conPlacaEnUso_lanzaErrEsc07SinGuardar() {
        when(vehiculoRepository.existePorPlaca("ABC-123")).thenReturn(true);

        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "Toyota", null, null)), EscuelaErrorCodes.PLACA_EN_USO);
        verify(vehiculoRepository, never()).guardar(any());
    }

    @Test
    void ejecutar_conDatosInvalidos_lanzaErrSys01SinTocarElRepositorio() {
        int anioFuturo = Year.now().getValue() + 2;
        esperaCodigo(() -> useCase.ejecutar(input(null, "Toyota", null, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ab", "Toyota", null, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("AB CD1", "Toyota", null, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "  ", null, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "T".repeat(41), null, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "Toyota", (short) 1979, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "Toyota", (short) anioFuturo, null)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(input("ABC-123", "Toyota", null, -1)), "ERR_SYS_01");
        esperaCodigo(() -> useCase.ejecutar(new RegistrarVehiculoInput(ESCUELA, "ABC-123", "Toyota", "Yaris", null,
            null, null, null, null, null, null)), "ERR_SYS_01");
        verify(vehiculoRepository, never()).existePorPlaca(any());
        verify(vehiculoRepository, never()).guardar(any());
    }
}
