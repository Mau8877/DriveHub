package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

@ExtendWith(MockitoExtension.class)
class DesactivarYReactivarEscuelaUseCaseTest {

    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private EscuelaRepository escuelaRepository;

    private static Escuela escuela(boolean activa) {
        return Escuela.builder().codigo(CODIGO).slug("autos-sur").nombre("Autos del Sur")
            .creadoEn(Instant.parse("2026-10-21T10:00:00Z")).eliminadaEn(activa ? null : Instant.now()).build();
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    // ------------------------------------------------------------------ desactivar

    @Test
    void desactivar_conEscuelaActiva_laDesactiva() {
        DesactivarEscuelaUseCase useCase = new DesactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela(true)));
        when(escuelaRepository.desactivar(CODIGO)).thenReturn(true);

        useCase.ejecutar(CODIGO);

        verify(escuelaRepository).desactivar(CODIGO);
    }

    @Test
    void desactivar_conEscuelaInexistente_lanzaErrEsc02() {
        DesactivarEscuelaUseCase useCase = new DesactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO);
        verify(escuelaRepository, never()).desactivar(CODIGO);
    }

    @Test
    void desactivar_conEscuelaYaInactiva_lanzaErrEsc04() {
        DesactivarEscuelaUseCase useCase = new DesactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela(false)));

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_YA_INACTIVA);
        verify(escuelaRepository, never()).desactivar(CODIGO);
    }

    @Test
    void desactivar_siOtraPeticionLaDesactivoPrimero_lanzaErrEsc04() {
        DesactivarEscuelaUseCase useCase = new DesactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela(true)));
        when(escuelaRepository.desactivar(CODIGO)).thenReturn(false);

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_YA_INACTIVA);
    }

    // ------------------------------------------------------------------ reactivar

    @Test
    void reactivar_conEscuelaInactiva_laReactivaYLaDevuelveActiva() {
        ReactivarEscuelaUseCase useCase = new ReactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO))
            .thenReturn(Optional.of(escuela(false)), Optional.of(escuela(true)));
        when(escuelaRepository.reactivar(CODIGO)).thenReturn(true);

        EscuelaOutput resultado = useCase.ejecutar(CODIGO);

        verify(escuelaRepository).reactivar(CODIGO);
        assertThat(resultado.estado()).isEqualTo(EstadoEscuela.ACTIVA);
        assertThat(resultado.slug()).isEqualTo("autos-sur");
    }

    @Test
    void reactivar_conEscuelaInexistente_lanzaErrEsc02() {
        ReactivarEscuelaUseCase useCase = new ReactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO);
        verify(escuelaRepository, never()).reactivar(CODIGO);
    }

    @Test
    void reactivar_conEscuelaYaActiva_lanzaErrEsc05() {
        ReactivarEscuelaUseCase useCase = new ReactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela(true)));

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_YA_ACTIVA);
        verify(escuelaRepository, never()).reactivar(CODIGO);
    }

    @Test
    void reactivar_siOtraPeticionLaReactivoPrimero_lanzaErrEsc05() {
        ReactivarEscuelaUseCase useCase = new ReactivarEscuelaUseCase(escuelaRepository);
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela(false)));
        when(escuelaRepository.reactivar(CODIGO)).thenReturn(false);

        esperaCodigo(() -> useCase.ejecutar(CODIGO), EscuelaErrorCodes.ESCUELA_YA_ACTIVA);
    }
}
