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
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.horarios.dto.DiaBloqueadoResponse;
import com.drivehub.academico.features.horarios.dto.FranjaDisponibleResponse;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.dto.InstructorDisponibleResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.service.FranjaDisponibleService;
import com.drivehub.academico.features.horarios.service.FranjaService;

import lombok.RequiredArgsConstructor;

/**
 * Franjas del instructor (CU12): verlas, bloquearlas y desbloquearlas, una por una o un día completo. Es la
 * vista del instructor sobre sus propias franjas. Además, la vista del estudiante (CU14): las franjas libres de
 * su escuela (`/disponibles`) y los instructores que tienen alguna (`/instructores`).
 */
@RestController
@RequestMapping("/api/v1/franjas")
@RequiredArgsConstructor
public class FranjaController {

    private final FranjaService service;
    private final FranjaDisponibleService disponibleService;
    private final BearerActual bearer;

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

    /**
     * Las franjas que el estudiante puede reservar (CU14). `filter[fecha]` manda sobre el rango
     * `filter[fechaDesde]`/`filter[fechaHasta]`; `filter[horaDesde]` es inclusiva y `filter[horaHasta]` exclusiva.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<ApiResponse<List<FranjaDisponibleResponse>>> disponibles(
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(name = "filter[fecha]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "filter[fechaDesde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name = "filter[fechaHasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(name = "filter[instructor]", required = false) UUID instructorCodigo,
            @RequestParam(name = "filter[horaDesde]", required = false) String horaDesde,
            @RequestParam(name = "filter[horaHasta]", required = false) String horaHasta,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        LocalDate desde = fecha != null ? fecha : fechaDesde;
        LocalDate hasta = fecha != null ? fecha : fechaHasta;
        Page<FranjaDisponibleResponse> pagina = disponibleService.listar(escuelaCodigo, bearer.cabecera(), desde,
            hasta, instructorCodigo, horaDesde, horaHasta, page, pageSize);
        PageMeta meta = new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
            pagina.getTotalPages());
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(),
            "Franjas disponibles obtenidas correctamente", meta));
    }

    /**
     * Los instructores con alguna franja disponible (CU14). Son pocos, así que va sin paginar: `meta` describe
     * una sola página con todos.
     */
    @GetMapping("/instructores")
    public ResponseEntity<ApiResponse<List<InstructorDisponibleResponse>>> instructores(
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(name = "filter[fecha]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "filter[fechaDesde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name = "filter[fechaHasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        LocalDate desde = fecha != null ? fecha : fechaDesde;
        LocalDate hasta = fecha != null ? fecha : fechaHasta;
        List<InstructorDisponibleResponse> instructores = disponibleService.instructores(escuelaCodigo,
            bearer.cabecera(), desde, hasta);
        PageMeta meta = new PageMeta(1, Math.max(instructores.size(), 1), instructores.size(),
            instructores.isEmpty() ? 0 : 1);
        return ResponseEntity.ok(ApiResponse.successList(instructores,
            "Instructores disponibles obtenidos correctamente", meta));
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
