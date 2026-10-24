package com.drivehub.escuela.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class VehiculoSeederTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private VehiculoSeeder seeder;

    @Test
    void run_sinAutos_creaOchoDeLaEscuelaDeEjemploConFotoDeCloudinaryYTodosLosEstados() {
        when(vehiculoRepository.existePorPlaca(any())).thenReturn(false);

        seeder.run(null);

        ArgumentCaptor<Vehiculo> creados = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehiculoRepository, times(8)).guardar(creados.capture());
        List<Vehiculo> autos = creados.getAllValues();
        assertThat(autos).allSatisfy(v -> {
            assertThat(v.getEscuelaCodigo()).isEqualTo(EscuelaSeeder.CODIGO_ESCUELA_EXAMPLE);
            assertThat(v.getImagenUrl()).startsWith("https://res.cloudinary.com/");
            assertThat(v.getCodigo()).isNotNull();
        });
        assertThat(autos.stream().map(Vehiculo::getPlaca).distinct()).hasSize(8);
        assertThat(autos.stream().map(Vehiculo::getImagenUrl).distinct()).hasSize(8);
        assertThat(autos.stream().map(Vehiculo::getEstado).distinct())
            .containsExactlyInAnyOrder(EstadoVehiculo.values());
    }

    @Test
    void run_conLosAutosYaCreados_noCreaNada() {
        when(vehiculoRepository.existePorPlaca(any())).thenReturn(true);

        seeder.run(null);

        verify(vehiculoRepository, never()).guardar(any());
    }
}
