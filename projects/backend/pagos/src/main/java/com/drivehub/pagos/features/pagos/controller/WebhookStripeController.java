package com.drivehub.pagos.features.pagos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.service.ConfirmarPagoService;

import lombok.RequiredArgsConstructor;

/**
 * Donde la pasarela avisa los cobros (CU27). Es público (sin JWT): su autenticación es la firma del aviso.
 *
 * * Recibe el cuerpo CRUDO: la firma se calcula sobre los bytes exactos que mandó la pasarela.
 * * Firma inválida: 400 `ERR_PAG_08`. Un error pasajero al procesar responde 5xx para que la pasarela reintente.
 */
@RestController
@RequestMapping("/api/v1/webhooks-stripe")
@RequiredArgsConstructor
public class WebhookStripeController {

    static final String CABECERA_FIRMA = "Stripe-Signature";

    private final PasarelaPago pasarela;
    private final ConfirmarPagoService confirmarPagoService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> recibir(
            @RequestHeader(name = CABECERA_FIRMA, required = false) String firma,
            @RequestBody String cuerpo) {
        confirmarPagoService.procesar(pasarela.verificarAviso(cuerpo, firma));
        return ResponseEntity.ok(ApiResponse.success(null, "Aviso recibido"));
    }
}
