package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ConsultarEstudiantesUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * Solo el instructor y el Administrador de escuela consultan estudiantes (`@PreAuthorize` en el servicio),
 * probado con la seguridad de métodos real y sin base de datos.
 */
@SpringJUnitConfig(ConsultarEstudiantesAutorizacionTest.Configuracion.class)
class ConsultarEstudiantesAutorizacionTest {

    private static final UUID SOLICITANTE = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        UsuarioRepositoryPort usuarioRepository() {
            UsuarioRepositoryPort repositorio = mock(UsuarioRepositoryPort.class);
            when(repositorio.buscarPorCodigo(SOLICITANTE)).thenReturn(Optional.of(
                Usuario.builder().codigo(SOLICITANTE).escuelaCodigo(ESCUELA).build()));
            when(repositorio.buscarEstudiantesPorCodigos(any(), any())).thenReturn(List.of());
            return repositorio;
        }

        @Bean
        ConsultarEstudiantesUseCase consultarEstudiantes(UsuarioRepositoryPort repositorio) {
            return new ConsultarEstudiantesService(repositorio, new EscuelaDelAdministrador(repositorio));
        }
    }

    @Autowired
    private ConsultarEstudiantesUseCase servicio;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_puedeConsultar() {
        assertThat(servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN_ESCUELA")
    void elAdministradorDeEscuela_puedeConsultar() {
        assertThat(servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"ESTUDIANTE", "SUPER_ADMIN"})
    void unEstudiante_ounSuperAdmin_noPuedenConsultar() {
        assertThatThrownBy(() -> servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID())))
            .isInstanceOf(AccessDeniedException.class);
    }
}
