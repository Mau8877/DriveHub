package com.drivehub.academico.features.horarios.controller;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.horarios.dto.CrearPlantillaHorarioRequest;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioCreadaResponse;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioResponse;
import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.service.PlantillaHorarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Horarios semanales del instructor (CU12). Solo el rol Instructor, y solo los suyos: la decisión
 * por rol vive en el service (@PreAuthorize) y el instructor sale del token (@CurrentUserId).
 */
@RestController
@RequestMapping("/api/v1/plantillas-horario")
@RequiredArgsConstructor
public class PlantillaHorarioController {

    private final PlantillaHorarioService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlantillaHorarioResponse>>> listar(
            @CurrentUserId UUID instructorCodigo,
            @RequestParam(name = "filter[estado]", required = false) List<EstadoPlantilla> estados,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Set<EstadoPlantilla> pedidos = estados == null || estados.isEmpty()
            ? Set.of() : EnumSet.copyOf(estados);
        Page<PlantillaHorarioResponse> pagina = service.listar(instructorCodigo, pedidos, page, pageSize);
        PageMeta meta = new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
            pagina.getTotalPages());
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Horarios obtenidos correctamente",
            meta));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlantillaHorarioCreadaResponse>> crear(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @Valid @RequestBody CrearPlantillaHorarioRequest request) {
        PlantillaHorarioCreadaResponse creada = service.crear(instructorCodigo, escuelaCodigo, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creada, "Horario creado correctamente"));
    }

    @PostMapping("/{codigo}/pausar")
    public ResponseEntity<ApiResponse<Void>> pausar(@CurrentUserId UUID instructorCodigo,
            @PathVariable UUID codigo) {
        service.pausar(instructorCodigo, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Horario pausado correctamente"));
    }

    @PostMapping("/{codigo}/reanudar")
    public ResponseEntity<ApiResponse<Void>> reanudar(@CurrentUserId UUID instructorCodigo,
            @PathVariable UUID codigo) {
        service.reanudar(instructorCodigo, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Horario reanudado correctamente"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivar(@CurrentUserId UUID instructorCodigo,
            @PathVariable UUID codigo) {
        service.desactivar(instructorCodigo, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Horario desactivado correctamente"));
    }
}
