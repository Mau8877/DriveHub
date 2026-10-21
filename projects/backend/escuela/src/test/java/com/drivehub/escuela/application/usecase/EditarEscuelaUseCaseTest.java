package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

@ExtendWith(MockitoExtension.class)
class EditarEscuelaUseCaseTest {

    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private EscuelaRepository escuelaRepository;

    @InjectMocks
    private EditarEscuelaUseCase useCase;

    private static Escuela escuela(String nombre, boolean activa) {
        return Escuela.builder().codigo(CODIGO).slug("autos-sur").nombre(nombre)
            .creadoEn(Instant.parse("2026-10-21T10:00:00Z")).eliminadaEn(activa ? null : Instant.now()).build();
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void ejecutar_conNombreValido_cambiaSoloElNombreYDejaElSlug() {
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela("Viejo", true)));
        when(escuelaRepository.actualizarNombre(CODIGO, "Nombre Nuevo"))
            .thenReturn(Optional.of(escuela("Nombre Nuevo", true)));

        EscuelaOutput resultado = useCase.ejecutar(CODIGO, "  Nombre Nuevo ");

        verify(escuelaRepository).actualizarNombre(CODIGO, "Nombre Nuevo");
        assertThat(resultado.nombre()).isEqualTo("Nombre Nuevo");
        assertThat(resultado.slug()).isEqualTo("autos-sur");
    }

    @Test
    void ejecutar_conNombreVacioOMuyCortoOMuyLargo_lanzaErrSys01SinTocarElRepositorio() {
        for (String malo : new String[] {null, "", "   ", "ab", "a".repeat(121)}) {
            esperaCodigo(() -> useCase.ejecutar(CODIGO, malo), "ERR_SYS_01");
        }
        verify(escuelaRepository, never()).buscarPorCodigo(any());
        verify(escuelaRepository, never()).actualizarNombre(any(), any());
    }

    @Test
    void ejecutar_conEscuelaInexistente_lanzaErrEsc02() {
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(CODIGO, "Nombre Nuevo"), EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO);
        verify(escuelaRepository, never()).actualizarNombre(any(), any());
    }

    @Test
    void ejecutar_conEscuelaInactiva_lanzaErrEsc06YNoCambiaNada() {
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela("Viejo", false)));

        esperaCodigo(() -> useCase.ejecutar(CODIGO, "Nombre Nuevo"), EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE);
        verify(escuelaRepository, never()).actualizarNombre(any(), any());
    }

    @Test
    void ejecutar_siSeDesactivaMientrasSeEdita_lanzaErrEsc06() {
        when(escuelaRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(escuela("Viejo", true)));
        when(escuelaRepository.actualizarNombre(CODIGO, "Nombre Nuevo")).thenReturn(Optional.empty());

        esperaCodigo(() -> useCase.ejecutar(CODIGO, "Nombre Nuevo"), EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE);
    }
}
