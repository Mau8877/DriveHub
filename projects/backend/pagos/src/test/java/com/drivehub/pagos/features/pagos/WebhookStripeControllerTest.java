package com.drivehub.pagos.features.pagos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.pagos.controller.WebhookStripeController;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.service.ConfirmarPagoService;

/**
 * El punto de entrada de los avisos de la pasarela (CU27): público (sin JWT), con el cuerpo crudo y la firma tal cual.
 */
@WebMvcTest(WebhookStripeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class WebhookStripeControllerTest {

    private static final String CUERPO = "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}";
    private static final String FIRMA = "t=1,v1=abc";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasarelaPago pasarela;

    @MockitoBean
    private ConfirmarPagoService confirmarPago;

    @Test
    void sinSesion_conFirmaValida_pasaElCuerpoCrudoYLaFirmaTalCual_yResponde200() throws Exception {
        AvisoPasarela aviso = AvisoPasarela.otro("evt_1");
        when(pasarela.verificarAviso(CUERPO, FIRMA)).thenReturn(aviso);

        mockMvc.perform(post("/api/v1/webhooks-stripe").header("Stripe-Signature", FIRMA)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Aviso recibido"))
            .andExpect(jsonPath("$.error").doesNotExist());

        verify(confirmarPago).procesar(aviso);
    }

    @Test
    void conFirmaInvalida_responde400ConErrPag08_yNoProcesaNada() throws Exception {
        when(pasarela.verificarAviso(any(), any())).thenThrow(
            new ValidationException(PagosErrorCodes.AVISO_FIRMA_INVALIDA, "El aviso no se pudo verificar"));

        mockMvc.perform(post("/api/v1/webhooks-stripe").header("Stripe-Signature", FIRMA)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_PAG_08"));

        verifyNoInteractions(confirmarPago);
    }

    @Test
    void sinLaCabeceraDeFirma_laVerificacionLaRechaza() throws Exception {
        when(pasarela.verificarAviso(CUERPO, null)).thenThrow(
            new ValidationException(PagosErrorCodes.AVISO_FIRMA_INVALIDA, "El aviso no se pudo verificar"));

        mockMvc.perform(post("/api/v1/webhooks-stripe").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_PAG_08"));
    }

    @Test
    void soloElPostEsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks-stripe")).andExpect(status().isUnauthorized());
    }
}
