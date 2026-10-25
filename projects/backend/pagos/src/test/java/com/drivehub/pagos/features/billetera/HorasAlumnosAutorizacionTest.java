package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.billetera.service.HorasAlumnosService;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;

/**
 * Solo el instructor ve las horas de sus alumnos (`@PreAuthorize` en el service), probado con la seguridad de
 * métodos real y sin base de datos ni servicios externos.
 */
@SpringJUnitConfig(HorasAlumnosAutorizacionTest.Configuracion.class)
class HorasAlumnosAutorizacionTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        HorasAlumnosService horasAlumnosService() {
            EscuelaClient escuela = mock(EscuelaClient.class);
            BearerActual bearer = mock(BearerActual.class);
            when(bearer.cabecera()).thenReturn("Bearer t");
            when(escuela.alumnosDelInstructor("Bearer t")).thenReturn(List.of());
            when(escuela.esMiAlumno(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
            return new HorasAlumnosService(escuela, mock(IdentidadClient.class), bearer, mock(PagoRepository.class),
                mock(BilleteraRepository.class), mock(MovimientoHorasRepository.class),
                mock(ReversionPagoRepository.class));
        }
    }

    @Autowired
    private HorasAlumnosService servicio;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_puedeListar() {
        assertThat(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null).getContent()).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"ESTUDIANTE", "ADMIN_ESCUELA", "SUPER_ADMIN"})
    void otroRol_noPuedeNiListarNiVerElDetalle() {
        assertThatThrownBy(() -> servicio.listar(ESCUELA, INSTRUCTOR, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.detalle(ESCUELA, INSTRUCTOR, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }
}
