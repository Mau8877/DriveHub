package com.drivehub.pagos.features.planes.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.exception.ForbiddenException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.response.ApiResponse;
import com.drivehub.pagos.common.response.PageMeta;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.features.planes.dto.CrearPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.EditarPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.PlanPagoResponse;
import com.drivehub.pagos.features.planes.service.PlanPagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Planes de horas de la escuela (CU25a): solo el Administrador de escuela. La decisión por rol vive en el
 * service (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL: así un Administrador no
 * * puede tocar los planes de otra escuela.
 */
@RestController
@RequestMapping("/api/v1/planes-pago")
@RequiredArgsConstructor
public class PlanPagoController {

    private final PlanPagoService planPagoService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlanPagoResponse>> crear(
            @CurrentEscuelaId UUID escuelaCodigo, @Valid @RequestBody CrearPlanPagoRequest request) {
        PlanPagoResponse creado = planPagoService.crear(escuelaDe(escuelaCodigo), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creado, "Plan creado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanPagoResponse>>> listar(
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[activo]", required = false) Boolean activo) {
        Page<PlanPagoResponse> pagina = planPagoService.listar(escuelaDe(escuelaCodigo), page, pageSize, search, activo);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Planes obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> obtener(
            @CurrentEscuelaId UUID escuelaCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            planPagoService.obtener(escuelaDe(escuelaCodigo), codigo), "Plan obtenido correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> editar(
            @CurrentEscuelaId UUID escuelaCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody EditarPlanPagoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            planPagoService.editar(escuelaDe(escuelaCodigo), codigo, request), "Plan actualizado correctamente"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> desactivar(
            @CurrentEscuelaId UUID escuelaCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            planPagoService.desactivar(escuelaDe(escuelaCodigo), codigo), "Plan desactivado correctamente"));
    }

    @PostMapping("/{codigo}/activar")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> activar(
            @CurrentEscuelaId UUID escuelaCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            planPagoService.activar(escuelaDe(escuelaCodigo), codigo), "Plan reactivado correctamente"));
    }

    // * Un Administrador siempre trae la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private static UUID escuelaDe(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(PagosErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
        return escuelaCodigo;
    }
}
