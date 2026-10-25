package com.drivehub.pagos.features.billetera.controller;

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
import com.drivehub.pagos.features.billetera.dto.ConsumirHorasRequest;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.dto.DevolverHorasRequest;
import com.drivehub.pagos.features.billetera.dto.SaldoResponse;
import com.drivehub.pagos.features.billetera.service.BilleteraService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * La billetera de horas (CU27). La decisión por rol vive en el service (@PreAuthorize), no acá.
 *
 * * La escuela siempre sale del token, nunca del cuerpo ni de la URL.
 * * Consumir y devolver llevan el header `Idempotency-Key`: repetirlos con la misma clave no gasta ni devuelve dos
 * * veces.
 */
@RestController
@RequestMapping("/api/v1/billeteras-horas")
@RequiredArgsConstructor
public class BilleteraController {

    static final String CABECERA_CLAVE = "Idempotency-Key";

    private final BilleteraService billeteraService;

    @GetMapping("/mia")
    public ResponseEntity<ApiResponse<SaldoResponse>> miSaldo(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo) {
        int horas = billeteraService.miSaldo(EscuelaDelToken.exigir(escuelaCodigo), estudianteCodigo);
        return ResponseEntity.ok(ApiResponse.success(new SaldoResponse(horas), "Saldo obtenido correctamente"));
    }

    @GetMapping("/{estudianteCodigo}/saldo")
    public ResponseEntity<ApiResponse<SaldoResponse>> saldoDe(
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable UUID estudianteCodigo) {
        int horas = billeteraService.saldoDe(EscuelaDelToken.exigir(escuelaCodigo), estudianteCodigo);
        return ResponseEntity.ok(ApiResponse.success(new SaldoResponse(horas), "Saldo obtenido correctamente"));
    }

    @PostMapping("/consumos")
    public ResponseEntity<ApiResponse<ConsumoResultado>> consumir(
            @CurrentUserId UUID solicitanteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestHeader(name = CABECERA_CLAVE, required = false) String idempotencyKey,
            @Valid @RequestBody ConsumirHorasRequest request) {
        ConsumoResultado resultado = billeteraService.consumir(EscuelaDelToken.exigir(escuelaCodigo),
            solicitanteCodigo, request.estudianteCodigo(), request.horas(), request.instructorCodigo(),
            request.claseCodigo(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(resultado, "Horas descontadas correctamente"));
    }

    @PostMapping("/devoluciones")
    public ResponseEntity<ApiResponse<DevolucionResultado>> devolver(
            @CurrentUserId UUID solicitanteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestHeader(name = CABECERA_CLAVE, required = false) String idempotencyKey,
            @Valid @RequestBody DevolverHorasRequest request) {
        DevolucionResultado resultado = billeteraService.devolver(EscuelaDelToken.exigir(escuelaCodigo),
            solicitanteCodigo, request.estudianteCodigo(), request.claseCodigo(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Horas devueltas correctamente"));
    }
}
