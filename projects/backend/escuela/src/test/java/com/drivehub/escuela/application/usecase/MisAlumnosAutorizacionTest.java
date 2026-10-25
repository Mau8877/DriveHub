package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * Solo el instructor ve sus alumnos (`@PreAuthorize` en los casos de uso), probado con la seguridad de
 * métodos real y sin base de datos.
 */
@SpringJUnitConfig(MisAlumnosAutorizacionTest.Configuracion.class)
class MisAlumnosAutorizacionTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        AsignacionAlumnoRepository asignaciones() {
            AsignacionAlumnoRepository repositorio = mock(AsignacionAlumnoRepository.class);
            when(repositorio.buscarEstudiantesDeInstructor(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0));
            when(repositorio.existeAsignacionActiva(any(), any(), any())).thenReturn(true);
            return repositorio;
        }

        @Bean
        ListarMisAlumnosUseCase listar(AsignacionAlumnoRepository asignaciones) {
            return new ListarMisAlumnosUseCase(asignaciones);
        }

        @Bean
        ConfirmarMiAlumnoUseCase confirmar(AsignacionAlumnoRepository asignaciones) {
            return new ConfirmarMiAlumnoUseCase(asignaciones);
        }
    }

    @Autowired
    private ListarMisAlumnosUseCase listar;

    @Autowired
    private ConfirmarMiAlumnoUseCase confirmar;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_puedeListarYConfirmar() {
        assertThat(listar.ejecutar(ESCUELA, INSTRUCTOR, null, null).elementos()).isEmpty();
        assertThat(confirmar.ejecutar(ESCUELA, INSTRUCTOR, UUID.randomUUID())).isNotNull();
    }

    @Test
    @WithMockUser(roles = {"ESTUDIANTE", "ADMIN_ESCUELA", "SUPER_ADMIN"})
    void otroRol_noPuedeNiListarNiConfirmar() {
        assertThatThrownBy(() -> listar.ejecutar(ESCUELA, INSTRUCTOR, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> confirmar.ejecutar(ESCUELA, INSTRUCTOR, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }
}
