package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.drivehub.pagos.features.comprobantes.service.TextoDeMotivoReversion;
import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

/**
 * El motivo en español que lleva el comprobante de reembolso (CU30 y CU31).
 */
class TextoDeMotivoReversionTest {

    private final TextoDeMotivoReversion texto = new TextoDeMotivoReversion();

    private static ReversionPago reversion(OrigenReversion origen, String motivo, String detalle) {
        return new ReversionPago(null, origen, motivo, detalle, BigDecimal.TEN, BigDecimal.ZERO);
    }

    @Test
    void cadaMotivoDelEstudiante_seMuestraConSuTextoEnEspanol() {
        for (MotivoReembolso motivo : MotivoReembolso.values()) {
            if (motivo == MotivoReembolso.OTRO) {
                continue;
            }
            assertThat(texto.de(reversion(OrigenReversion.ESTUDIANTE, motivo.name(), null)))
                .isEqualTo(motivo.getTexto());
        }
    }

    @Test
    void conOtro_seMuestraLoQueEscribioElEstudiante() {
        assertThat(texto.de(reversion(OrigenReversion.ESTUDIANTE, "OTRO", "  Perdí mi trabajo  ")))
            .isEqualTo("Perdí mi trabajo");
    }

    @Test
    void unaCompensacion_diceQueLaCompraNoSePudoCompletar_seaCualSeaElMotivo() {
        assertThat(texto.de(reversion(OrigenReversion.SAGA, "MONTO_DISTINTO", null)))
            .isEqualTo("La compra no se pudo completar");
    }

    @Test
    void unMotivoDesconocido_caeEnElTextoDeLaCompensacion() {
        assertThat(texto.de(reversion(OrigenReversion.ESTUDIANTE, "RARO", null)))
            .isEqualTo("La compra no se pudo completar");
    }
}
