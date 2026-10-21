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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RevertirAdministradorEscuelaServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @InjectMocks
    private RevertirAdministradorEscuelaService servicio;

    private Usuario administrador(Instant ultimoLogin) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("marta@gmail.com").escuelaCodigo(ESCUELA)
            .ultimoLogin(ultimoLogin).build();
    }

    @Test
    void revertir_sinAdministradorEnLaEscuela_noHaceNada() {
        when(usuarioRepository.buscarAdministradorDeEscuela(ESCUELA)).thenReturn(Optional.empty());

        servicio.revertir(ESCUELA);

        verify(usuarioRepository, never()).eliminarDefinitivamente(any());
    }

    @Test
    void revertir_conAdministradorQueNuncaIngreso_loBorraDeFormaDefinitiva() {
        Usuario admin = administrador(null);
        when(usuarioRepository.buscarAdministradorDeEscuela(ESCUELA)).thenReturn(Optional.of(admin));

        servicio.revertir(ESCUELA);

        verify(usuarioRepository).eliminarDefinitivamente(admin.getCodigo());
    }

    @Test
    void revertir_conAdministradorQueYaInicioSesion_lanzaErrIde13YNoBorraNada() {
        when(usuarioRepository.buscarAdministradorDeEscuela(ESCUELA))
            .thenReturn(Optional.of(administrador(Instant.now())));

        assertThatThrownBy(() -> servicio.revertir(ESCUELA))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ADMIN_EN_USO));
        verify(usuarioRepository, never()).eliminarDefinitivamente(any());
    }
}
