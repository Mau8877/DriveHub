package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class RevertirCuentaInstructorServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private RevertirCuentaInstructorService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new RevertirCuentaInstructorService(new EscuelaDelAdministrador(usuarioRepository), usuarioRepository);
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private Usuario instructor(Instant ultimoLogin) {
        return Usuario.builder().codigo(INSTRUCTOR).email("ivan@gmail.com").escuelaCodigo(ESCUELA)
            .ultimoLogin(ultimoLogin).build();
    }

    @Test
    void revertir_conUnaCuentaQueNuncaIngreso_laBorraDeFormaDefinitiva() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.of(instructor(null)));

        servicio.revertir(ADMIN, INSTRUCTOR);

        verify(usuarioRepository).eliminarDefinitivamente(INSTRUCTOR);
    }

    @Test
    void revertir_sinLaCuentaEnLaEscuela_noHaceNada() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.empty());

        servicio.revertir(ADMIN, INSTRUCTOR);

        verify(usuarioRepository, never()).eliminarDefinitivamente(any());
    }

    @Test
    void revertir_conUnaCuentaQueYaInicioSesion_lanzaErrIde23YNoBorra() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR))
            .thenReturn(Optional.of(instructor(Instant.parse("2026-10-22T10:00:00Z"))));

        assertThatThrownBy(() -> servicio.revertir(ADMIN, INSTRUCTOR))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CUENTA_YA_USADA);
                assertThat(e.getHttpStatus().value()).isEqualTo(409);
            });
        verify(usuarioRepository, never()).eliminarDefinitivamente(any());
    }
}
