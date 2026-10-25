package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.features.comprobantes.service.ComprobanteListener;
import com.drivehub.pagos.features.comprobantes.service.ComprobanteService;
import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;

/**
 * El comprobante se pide al confirmarse el pago (CU30): solo después del commit. Se prueba con la maquinaria
 * transaccional real de Spring y una transacción simulada, sin base de datos.
 */
@SpringJUnitConfig(ComprobanteListenerTest.Configuracion.class)
class ComprobanteListenerTest {

    /** Una transacción que no toca nada, pero con sus fases (commit y rollback) reales para los eventos. */
    static class TransaccionSimulada extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // * Nada que abrir
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // * Nada que confirmar
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // * Nada que deshacer
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class Configuracion {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TransaccionSimulada();
        }

        @Bean
        ComprobanteService comprobanteService() {
            return mock(ComprobanteService.class);
        }

        @Bean
        ComprobanteListener comprobanteListener(ComprobanteService servicio) {
            return new ComprobanteListener(servicio);
        }
    }

    @Autowired
    private ApplicationEventPublisher eventos;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private ComprobanteService servicio;

    @BeforeEach
    void reiniciar() {
        org.mockito.Mockito.reset(servicio);
    }

    @Test
    void alConfirmarseElPago_generaElComprobante_soloDespuesDelCommit() {
        UUID pago = UUID.randomUUID();

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            eventos.publishEvent(new PagoConfirmado(pago));
            verifyNoInteractions(servicio);
        });

        verify(servicio).generarDePago(pago);
    }

    @Test
    void siLaConfirmacionSeDeshace_noSeGeneraNada() {
        UUID pago = UUID.randomUUID();

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            eventos.publishEvent(new PagoConfirmado(pago));
            estado.setRollbackOnly();
        });

        verify(servicio, never()).generarDePago(any());
    }

    @Test
    void elMismoAvisoDosVeces_pideElComprobanteDosVeces_yElServicioDejaUno() {
        UUID pago = UUID.randomUUID();

        for (int i = 0; i < 2; i++) {
            new TransactionTemplate(transacciones)
                .executeWithoutResult(estado -> eventos.publishEvent(new PagoConfirmado(pago)));
        }

        // * La idempotencia es del servicio (ComprobanteServiceTest): acá solo se comprueba que se le pide cada vez
        verify(servicio, times(2)).generarDePago(pago);
    }

    @Test
    void unaExcepcionDelServicio_nuncaSale() {
        UUID pago = UUID.randomUUID();
        doThrow(new IllegalStateException("falla")).when(servicio).generarDePago(pago);

        assertThatCode(() -> new TransactionTemplate(transacciones)
            .executeWithoutResult(estado -> eventos.publishEvent(new PagoConfirmado(pago))))
            .doesNotThrowAnyException();
    }
}
