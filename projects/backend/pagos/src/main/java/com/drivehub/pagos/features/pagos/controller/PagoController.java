package com.drivehub.pagos.features.pagos.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.common.util.EscuelaDelToken;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.config.CurrentUserId;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.dto.IniciarPagoRequest;
import com.drivehub.pagos.features.pagos.dto.PagoIniciadoResponse;
import com.drivehub.pagos.features.pagos.service.PagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Los pagos del estudiante: comprar un plan de horas (CU26). La decisión por rol vive en el service
 * (@PreAuthorize), no acá.
 *
 * * El estudiante y su escuela salen del token, nunca del cuerpo ni de la URL.
 * * La compra lleva el header `Idempotency-Key`: reintentarla con la misma clave devuelve el mismo pago.
 */
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    static final String CABECERA_CLAVE = "Idempotency-Key";

    private final PagoService pagoService;

    /**
     * 201 si el pago se creó con esta petición; 200 si es el de un reintento con la misma clave.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PagoIniciadoResponse>> iniciar(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestHeader(name = CABECERA_CLAVE, required = false) String idempotencyKey,
            @Valid @RequestBody IniciarPagoRequest request) {
        PagoService.ResultadoIniciar resultado = pagoService.iniciar(EscuelaDelToken.exigir(escuelaCodigo),
            estudianteCodigo, request.planCodigo(), idempotencyKey);
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado)
            .body(ApiResponse.success(resultado.pago(), "Pago iniciado correctamente"));
    }

    @GetMapping("/{codigo}/estado")
    public ResponseEntity<ApiResponse<EstadoPagoResponse>> estado(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            pagoService.estado(EscuelaDelToken.exigir(escuelaCodigo), estudianteCodigo, codigo),
            "Estado obtenido correctamente"));
    }
}
