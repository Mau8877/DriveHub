package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.domain.gateway.FirmaSubidaImagen;
import com.drivehub.escuela.domain.gateway.ImagenesGateway;

@ExtendWith(MockitoExtension.class)
class FirmarSubidaImagenVehiculoUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private ImagenesGateway imagenesGateway;

    @InjectMocks
    private FirmarSubidaImagenVehiculoUseCase useCase;

    @Test
    void ejecutar_pideLaFirmaParaLaCarpetaDeLaEscuela() {
        String carpeta = "drivehub/" + ESCUELA + "/vehiculos";
        FirmaSubidaImagen esperada = new FirmaSubidaImagen("nube", "key", 1L, carpeta, "firma");
        when(imagenesGateway.firmarSubida(carpeta)).thenReturn(esperada);

        assertThat(useCase.ejecutar(ESCUELA)).isSameAs(esperada);
        verify(imagenesGateway).firmarSubida(carpeta);
    }
}
