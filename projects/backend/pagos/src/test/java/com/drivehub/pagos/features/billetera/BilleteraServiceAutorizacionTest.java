package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;

/**
 * Quién puede gastar y devolver horas (CU27): el instructor, el Administrador de la escuela y el estudiante
 * solo sobre su propia billetera. Se prueba con la seguridad de métodos real, sin base de datos.
 */
@SpringJUnitConfig(BilleteraServiceAutorizacionTest.Configuracion.class)
class BilleteraServiceAutorizacionTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID OTRO_ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    @Configuration
    @EnableMethodSecurity
    static class Configuracion {

        @Bean
        BilleteraService billeteraService() {
            BilleteraRepository billeteras = mock(BilleteraRepository.class);
            // * Sin billetera: si la autorización deja pasar, el servicio responde ERR_PAG_09 (una BusinessException)
            when(billeteras.buscarParaActualizar(any(), any())).thenReturn(Optional.empty());
            return new BilleteraService(billeteras, mock(MovimientoHorasRepository.class), mock(PagoRepository.class));
        }
    }

    @Autowired
    private BilleteraService servicio;

    private void consumirComo(UUID solicitante, UUID estudiante) {
        servicio.consumir(ESCUELA, solicitante, estudiante, 1, INSTRUCTOR, CLASE, "clave");
    }

    private void devolverComo(UUID solicitante, UUID estudiante) {
        servicio.devolver(ESCUELA, solicitante, estudiante, CLASE, "clave");
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void unInstructor_puedeGastarYDevolverSobreCualquierEstudiante() {
        assertThatThrownBy(() -> consumirComo(INSTRUCTOR, ESTUDIANTE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> devolverComo(INSTRUCTOR, ESTUDIANTE)).isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN_ESCUELA")
    void elAdministradorDeLaEscuela_puedeGastarYDevolver() {
        assertThatThrownBy(() -> consumirComo(UUID.randomUUID(), ESTUDIANTE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> devolverComo(UUID.randomUUID(), ESTUDIANTE)).isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_puedeGastarYDevolverSobreSuPropiaBilletera() {
        assertThatThrownBy(() -> consumirComo(ESTUDIANTE, ESTUDIANTE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> devolverComo(ESTUDIANTE, ESTUDIANTE)).isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void unEstudiante_noPuedeTocarLaBilleteraDeOtro() {
        assertThatThrownBy(() -> consumirComo(ESTUDIANTE, OTRO_ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> devolverComo(ESTUDIANTE, OTRO_ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void otroRol_noPuedeNada() {
        assertThatThrownBy(() -> consumirComo(ESTUDIANTE, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> devolverComo(ESTUDIANTE, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sinSesion_noPuedeNada() {
        assertThatThrownBy(() -> consumirComo(ESTUDIANTE, ESTUDIANTE)).isInstanceOfAny(
            AccessDeniedException.class, org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void elSaldoPropio_loVeElEstudiante_yElDeOtro_noEsDeEl() {
        org.assertj.core.api.Assertions.assertThat(servicio.miSaldo(ESCUELA, ESTUDIANTE)).isZero();
        assertThatThrownBy(() -> servicio.saldoDe(ESCUELA, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = {"INSTRUCTOR"})
    void elSaldoDeUnEstudiante_loVeElInstructor_peroNoEsElSaldoPropio() {
        org.assertj.core.api.Assertions.assertThat(servicio.saldoDe(ESCUELA, ESTUDIANTE)).isZero();
        assertThatThrownBy(() -> servicio.miSaldo(ESCUELA, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN_ESCUELA")
    void elSaldoDeUnEstudiante_loVeElAdministradorDeLaEscuela() {
        org.assertj.core.api.Assertions.assertThat(servicio.saldoDe(ESCUELA, ESTUDIANTE)).isZero();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void otroRol_noVeNingunSaldo() {
        assertThatThrownBy(() -> servicio.miSaldo(ESCUELA, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.saldoDe(ESCUELA, ESTUDIANTE)).isInstanceOf(AccessDeniedException.class);
    }
}
