package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

@ExtendWith(MockitoExtension.class)
class ObtenerEscuelaPorSlugUseCaseTest {

    @Mock
    private EscuelaRepository escuelaRepository;

    @InjectMocks
    private ObtenerEscuelaPorSlugUseCase useCase;

    @Test
    void ejecutar_conEscuelaActiva_devuelveSusDatosPublicos() {
        UUID codigo = UUID.randomUUID();
        when(escuelaRepository.buscarActivaPorSlug("escuelaexample")).thenReturn(Optional.of(
            Escuela.builder().codigo(codigo).slug("escuelaexample").nombre("Escuela Example")
                .estado(EstadoEscuela.ACTIVA).build()));

        EscuelaPublicaOutput resultado = useCase.ejecutar("escuelaexample");

        assertThat(resultado.codigo()).isEqualTo(codigo);
        assertThat(resultado.slug()).isEqualTo("escuelaexample");
        assertThat(resultado.nombre()).isEqualTo("Escuela Example");
    }

    @Test
    void ejecutar_conSlugInexistenteOEscuelaInactiva_lanzaNotFoundConErrEsc01() {
        when(escuelaRepository.buscarActivaPorSlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("no-existe"))
            .isInstanceOfSatisfying(NotFoundException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA));
    }

    @Test
    void ejecutar_conMayusculasYEspacios_normalizaElSlugAntesDeBuscar() {
        when(escuelaRepository.buscarActivaPorSlug("escuelaexample")).thenReturn(Optional.of(
            Escuela.builder().codigo(UUID.randomUUID()).slug("escuelaexample").nombre("Escuela Example")
                .estado(EstadoEscuela.ACTIVA).build()));

        useCase.ejecutar("  EscuelaExample ");

        verify(escuelaRepository).buscarActivaPorSlug("escuelaexample");
    }
}
