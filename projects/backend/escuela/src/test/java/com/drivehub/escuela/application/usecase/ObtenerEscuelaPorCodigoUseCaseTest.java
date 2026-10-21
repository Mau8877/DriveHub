package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EscuelaEstadoOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

@ExtendWith(MockitoExtension.class)
class ObtenerEscuelaPorCodigoUseCaseTest {

    @Mock
    private EscuelaRepository escuelaRepository;

    @InjectMocks
    private ObtenerEscuelaPorCodigoUseCase useCase;

    private Escuela escuela(UUID codigo, boolean activa) {
        return Escuela.builder().codigo(codigo).slug("escuelaexample").nombre("Escuela Example")
            .eliminadaEn(activa ? null : Instant.now()).build();
    }

    @Test
    void ejecutar_conEscuelaActiva_devuelveActivaVerdadera() {
        UUID codigo = UUID.randomUUID();
        when(escuelaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(escuela(codigo, true)));

        EscuelaEstadoOutput resultado = useCase.ejecutar(codigo);

        assertThat(resultado.activa()).isTrue();
        assertThat(resultado.slug()).isEqualTo("escuelaexample");
    }

    @Test
    void ejecutar_conEscuelaSuspendida_laDevuelveConActivaFalsa() {
        UUID codigo = UUID.randomUUID();
        when(escuelaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(escuela(codigo, false)));

        assertThat(useCase.ejecutar(codigo).activa()).isFalse();
    }

    @Test
    void ejecutar_conCodigoInexistente_lanzaNotFoundConErrEsc02() {
        UUID codigo = UUID.randomUUID();
        when(escuelaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(codigo))
            .isInstanceOfSatisfying(NotFoundException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO));
    }
}
