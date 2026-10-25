package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.ReembolsoCalculadora;

/**
 * El historial de compras es solo del estudiante (`@PreAuthorize` en el service), probado con la seguridad de
 * métodos real y sin base de datos.
 */
@SpringJUnitConfig(ConsultaPagosServiceAutorizacionTest.Configuracion.class)
class ConsultaPagosServiceAutorizacionTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        @SuppressWarnings("unchecked")
        PagoRepository pagoRepository() {
            PagoRepository repositorio = mock(PagoRepository.class);
            when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
            return repositorio;
        }

        @Bean
        ConsultaPagosService consultaPagosService(PagoRepository pagos) {
            return new ConsultaPagosService(pagos, mock(ReversionPagoRepository.class),
                new ReembolsoCalculadora(PagosPropertiesPrueba.conRetencion(5)));
        }
    }

    @Autowired
    private ConsultaPagosService servicio;

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_siPuedeVerSuHistorial() {
        assertThat(servicio.listar(ESCUELA, ESTUDIANTE, null, null, null).getContent()).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"INSTRUCTOR", "ADMIN_ESCUELA", "SUPER_ADMIN"})
    void quienNoEsEstudiante_noPuedeListarNiVerElDetalle() {
        assertThatThrownBy(() -> servicio.listar(ESCUELA, ESTUDIANTE, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.detalle(ESCUELA, ESTUDIANTE, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }
}
