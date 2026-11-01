package com.drivehub.pagos.features.billetera.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.features.billetera.dto.ConsumoInternoRequest;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionInternaRequest;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.service.BilleteraService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Descontar y devolver horas SIN sesión (CU44): lo usa el orquestador de Académico, que corre sin ningún estudiante
 * conectado. Las mismas reglas que las rutas públicas de la billetera.
 *
 * * Solo pasa quien manda la credencial de servicio (`CredencialServicioFilter`); un JWT no sirve acá.
 * * Está FUERA de `/api/v1` y NO se registra en el gateway: el navegador y la app nunca las alcanzan.
 * * Sin `@CurrentUserId` ni `@CurrentEscuelaId`: no hay token, la escuela y el estudiante vienen en el cuerpo.
 * ! Nunca agregar acá otra operación sin decidirlo: la credencial de servicio solo permite descontar y devolver.
 */
@RestController
@RequestMapping("/internal/v1/billeteras-horas")
@RequiredArgsConstructor
public class BilleteraInternaController {

    static final String CABECERA_CLAVE = "Idempotency-Key";

    private final BilleteraService billeteraService;

    @PostMapping("/consumos")
    public ResponseEntity<ApiResponse<ConsumoResultado>> consumir(
            @RequestHeader(name = CABECERA_CLAVE, required = false) String idempotencyKey,
            @Valid @RequestBody ConsumoInternoRequest request) {
        ConsumoResultado resultado = billeteraService.consumirDeSistema(request.escuelaCodigo(),
            request.estudianteCodigo(), request.horas(), request.instructorCodigo(), request.claseCodigo(),
            idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(resultado, "Horas descontadas correctamente"));
    }

    @PostMapping("/devoluciones")
    public ResponseEntity<ApiResponse<DevolucionResultado>> devolver(
            @RequestHeader(name = CABECERA_CLAVE, required = false) String idempotencyKey,
            @Valid @RequestBody DevolucionInternaRequest request) {
        DevolucionResultado resultado = billeteraService.devolverDeSistema(request.escuelaCodigo(),
            request.estudianteCodigo(), request.claseCodigo(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Horas devueltas correctamente"));
    }
}
