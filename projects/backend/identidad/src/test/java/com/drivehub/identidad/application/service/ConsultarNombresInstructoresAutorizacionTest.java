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
import com.drivehub.identidad.domain.port.in.ConsultarNombresInstructoresUseCase;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * Solo el estudiante y el instructor consultan nombres de instructores (`@PreAuthorize` en el servicio),
 * probado con la seguridad de métodos real y sin base de datos.
 */
@SpringJUnitConfig(ConsultarNombresInstructoresAutorizacionTest.Configuracion.class)
class ConsultarNombresInstructoresAutorizacionTest {

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
            when(repositorio.buscarInstructoresPorCodigos(any(), any())).thenReturn(List.of());
            return repositorio;
        }

        @Bean
        ConsultarNombresInstructoresUseCase consultarNombres(UsuarioRepositoryPort repositorio) {
            return new ConsultarNombresInstructoresService(repositorio, new EscuelaDelAdministrador(repositorio));
        }
    }

    @Autowired
    private ConsultarNombresInstructoresUseCase servicio;

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_puedeConsultar() {
        assertThat(servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_puedeConsultar() {
        assertThat(servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"ADMIN_ESCUELA", "SUPER_ADMIN"})
    void elAdministrador_oElSuperAdmin_noPuedenConsultarPorAca() {
        assertThatThrownBy(() -> servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID())))
            .isInstanceOf(AccessDeniedException.class);
    }
}
