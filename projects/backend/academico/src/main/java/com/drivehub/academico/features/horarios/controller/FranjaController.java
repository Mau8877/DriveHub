package com.drivehub.academico.features.horarios.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.horarios.dto.DiaBloqueadoResponse;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.service.FranjaService;

import lombok.RequiredArgsConstructor;

/**
 * Franjas del instructor (CU12): verlas, bloquearlas y desbloquearlas, una por una o un día completo. Es la
 * vista del instructor sobre sus propias franjas; reservar o consultar disponibilidad llega con sus propios
 * casos de uso.
 */
@RestController
@RequestMapping("/api/v1/franjas")
@RequiredArgsConstructor
public class FranjaController {

    private final FranjaService service;

    /**
     * `filter[fecha]` es un día exacto; `filter[fechaDesde]` y `filter[fechaHasta]` son un rango. Si llega
     * `filter[fecha]`, manda sobre el rango.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FranjaResponse>>> listar(
            @CurrentUserId UUID instructorCodigo,
            @RequestParam(name = "filter[fecha]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "filter[fechaDesde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name = "filter[fechaHasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(name = "filter[estado]", required = false) EstadoFranja estado,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        LocalDate desde = fecha != null ? fecha : fechaDesde;
        LocalDate hasta = fecha != null ? fecha : fechaHasta;
        Page<FranjaResponse> pagina = service.listar(instructorCodigo, desde, hasta, estado, page, pageSize);
        PageMeta meta = new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
            pagina.getTotalPages());
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Franjas obtenidas correctamente",
            meta));
    }

    @PostMapping("/{codigo}/bloquear")
    public ResponseEntity<ApiResponse<FranjaResponse>> bloquear(@CurrentUserId UUID instructorCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(service.bloquear(instructorCodigo, codigo),
            "Franja bloqueada correctamente"));
    }

    @PostMapping("/{codigo}/desbloquear")
    public ResponseEntity<ApiResponse<FranjaResponse>> desbloquear(@CurrentUserId UUID instructorCodigo,
            @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(service.desbloquear(instructorCodigo, codigo),
            "Franja desbloqueada correctamente"));
    }

    @PostMapping("/dias/{fecha}/bloquear")
    public ResponseEntity<ApiResponse<DiaBloqueadoResponse>> bloquearDia(@CurrentUserId UUID instructorCodigo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.success(service.bloquearDia(instructorCodigo, fecha),
            "Día bloqueado correctamente"));
    }

    @PostMapping("/dias/{fecha}/desbloquear")
    public ResponseEntity<ApiResponse<DiaBloqueadoResponse>> desbloquearDia(@CurrentUserId UUID instructorCodigo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.success(service.desbloquearDia(instructorCodigo, fecha),
            "Día desbloqueado correctamente"));
    }
}
