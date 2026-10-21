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
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.service.FranjaService;

import lombok.RequiredArgsConstructor;

/**
 * Franjas del instructor (CU12): verlas, bloquearlas y desbloquearlas. Es la vista del instructor
 * sobre sus propias franjas; reservar o consultar disponibilidad llega con sus propios casos de uso.
 */
@RestController
@RequestMapping("/api/v1/franjas")
@RequiredArgsConstructor
public class FranjaController {

    private final FranjaService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FranjaResponse>>> listar(
            @CurrentUserId UUID instructorCodigo,
            @RequestParam(name = "filter[fecha]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "filter[estado]", required = false) EstadoFranja estado,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<FranjaResponse> pagina = service.listar(instructorCodigo, fecha, estado, page, pageSize);
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
}
