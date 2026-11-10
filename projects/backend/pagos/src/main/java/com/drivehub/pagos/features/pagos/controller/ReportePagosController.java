package com.drivehub.pagos.features.pagos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.common.response.PageMeta;
import com.drivehub.pagos.common.util.EscuelaDelToken;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.features.pagos.dto.ReportePagoResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.service.ReportePagosService;

import lombok.RequiredArgsConstructor;

/**
 * Las compras de horas de la escuela, para los reportes del Administrador (CU52). Solo lectura. La decisión por rol
 * vive en el service (@PreAuthorize).
 */
@RestController
@RequestMapping("/api/v1/reportes-pagos")
@RequiredArgsConstructor
public class ReportePagosController {

    private final ReportePagosService service;

    /** `filter[desde]` y `filter[hasta]` en `yyyy-MM-dd`, días de Bolivia e inclusivos. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportePagoResponse>>> listar(
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[desde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "filter[hasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "filter[estado]", required = false) EstadoPagoVisible estado) {
        Page<ReportePagoResponse> pagina = service.listar(EscuelaDelToken.exigir(escuelaCodigo), desde, hasta, estado,
            page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Pagos obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }
}
