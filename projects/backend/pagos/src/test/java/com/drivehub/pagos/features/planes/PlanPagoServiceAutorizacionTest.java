package com.drivehub.pagos.features.planes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
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
import com.drivehub.pagos.features.planes.dto.CrearPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.EditarPlanPagoRequest;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;
import com.drivehub.pagos.features.planes.service.PlanPagoService;

/**
 * La decisión por rol vive en el service (`@PreAuthorize`): solo el Administrador de escuela gestiona planes.
 * Se prueba con la seguridad de métodos real, sin base de datos.
 */
@SpringJUnitConfig(PlanPagoServiceAutorizacionTest.Configuracion.class)
class PlanPagoServiceAutorizacionTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        @SuppressWarnings("unchecked")
        PlanPagoRepository planRepository() {
            PlanPagoRepository repositorio = mock(PlanPagoRepository.class);
            when(repositorio.findByCodigoAndEscuelaCodigo(any(), any())).thenReturn(
                Optional.of(new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00"))));
            when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
            return repositorio;
        }

        @Bean
        PlanPagoService planPagoService(PlanPagoRepository repositorio) {
            return new PlanPagoService(repositorio, new ConversionMoneda(PagosPropertiesPrueba.con(PagosPropertiesPrueba.TIPO_CAMBIO)));
        }
    }

    @Autowired
    private PlanPagoService servicio;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_noPuedeHacerNinguna_deLasSeisOperaciones() {
        assertThatThrownBy(() -> servicio.crear(ESCUELA, new CrearPlanPagoRequest("X", 1, BigDecimal.ONE)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.listar(ESCUELA, null, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.obtener(ESCUELA, CODIGO)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest("X", null, null)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.desactivar(ESCUELA, CODIGO)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.activar(ESCUELA, CODIGO)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_tampocoPuede() {
        assertThatThrownBy(() -> servicio.obtener(ESCUELA, CODIGO)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void elSuperAdmin_noGestionaPlanesDeUnaEscuela() {
        assertThatThrownBy(() -> servicio.listar(ESCUELA, null, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN_ESCUELA")
    void elAdministradorDeEscuela_si_puede() {
        assertThat(servicio.obtener(ESCUELA, CODIGO).nombre()).isEqualTo("Plan 10");
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_puedeVerLosPlanesDisponibles() {
        assertThat(servicio.disponibles(ESCUELA, null, null).getContent()).isEmpty();
    }

    @Test
    @WithMockUser(roles = {"ADMIN_ESCUELA", "INSTRUCTOR"})
    void quienNoEsEstudiante_noPuedeVerLosPlanesDisponibles() {
        assertThatThrownBy(() -> servicio.disponibles(ESCUELA, null, null))
            .isInstanceOf(AccessDeniedException.class);
    }
}
