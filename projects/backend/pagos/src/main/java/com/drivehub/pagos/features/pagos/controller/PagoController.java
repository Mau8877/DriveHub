package com.drivehub.pagos.features.pagos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.common.response.PageMeta;
import com.drivehub.pagos.common.util.EscuelaDelToken;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.config.CurrentUserId;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.dto.IniciarPagoRequest;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.dto.PagoIniciadoResponse;
import com.drivehub.pagos.features.pagos.dto.PagoResumenResponse;
import com.drivehub.pagos.features.pagos.dto.SolicitarReembolsoRequest;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.PagoService;
import com.drivehub.pagos.features.pagos.service.ReversionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Los pagos del estudiante: comprar un plan de horas (CU26) y ver su historial (CU28). pedir el reembolso (CU31). La decisión por rol vive en el service
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
    private final ConsultaPagosService consultaPagosService;
    private final ReversionService reversionService;

    /**
     * El historial de compras (CU28). Un `filter[estado]` desconocido es 400 `ERR_SYS_01`.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PagoResumenResponse>>> listar(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[estado]", required = false) EstadoPagoVisible estado) {
        Page<PagoResumenResponse> pagina = consultaPagosService.listar(EscuelaDelToken.exigir(escuelaCodigo),
            estudianteCodigo, page, pageSize, estado);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Pagos obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<PagoDetalleResponse>> detalle(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            consultaPagosService.detalle(EscuelaDelToken.exigir(escuelaCodigo), estudianteCodigo, codigo),
            "Pago obtenido correctamente"));
    }

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

    /**
     * 202: el reembolso quedó pedido (el detalle viene como "reembolso en proceso"); termina cuando la pasarela
     * avisa (CU31).
     */
    @PostMapping("/{codigo}/reembolso")
    public ResponseEntity<ApiResponse<PagoDetalleResponse>> reembolsar(
            @CurrentUserId UUID estudianteCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable UUID codigo,
            @Valid @RequestBody SolicitarReembolsoRequest request) {
        PagoDetalleResponse detalle = reversionService.solicitarReembolso(EscuelaDelToken.exigir(escuelaCodigo),
            estudianteCodigo, codigo, request.motivo(), request.motivoDetalle());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(detalle, "Reembolso solicitado correctamente"));
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
