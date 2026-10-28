package com.drivehub.academico.features.simulacros.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.features.simulacros.dto.CrearSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.EditarSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.SimulacroResponse;
import com.drivehub.academico.features.simulacros.repository.FiltroSimulacros;
import com.drivehub.academico.features.simulacros.service.SimulacroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Configuración de simulacros escritos (CU20): solo el Administrador de escuela. La decisión por rol vive
 * en el service (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL: así un
 * * Administrador no puede tocar los simulacros de otra escuela.
 */
@RestController
@RequestMapping("/api/v1/simulacros")
@RequiredArgsConstructor
public class SimulacroController {

    private final SimulacroService simulacroService;

    @PostMapping
    public ResponseEntity<ApiResponse<SimulacroResponse>> crear(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearSimulacroRequest request) {
        SimulacroResponse creado = simulacroService.crear(escuelaDe(jwt), autorDe(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creado, "Simulacro creado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SimulacroResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[categoria]", required = false) String categoria,
            @RequestParam(name = "filter[activo]", required = false) Boolean activo) {
        Page<SimulacroResponse> pagina = simulacroService.listar(escuelaDe(jwt),
            new FiltroSimulacros(search, categoria, activo), page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Simulacros obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<SimulacroResponse>> obtener(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            simulacroService.obtener(escuelaDe(jwt), codigo), "Simulacro obtenido correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<SimulacroResponse>> editar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo,
            @Valid @RequestBody EditarSimulacroRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            simulacroService.editar(escuelaDe(jwt), codigo, request), "Simulacro actualizado correctamente"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<SimulacroResponse>> desactivar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            simulacroService.cambiarActivo(escuelaDe(jwt), codigo, false), "Simulacro desactivado correctamente"));
    }

    @PostMapping("/{codigo}/activar")
    public ResponseEntity<ApiResponse<SimulacroResponse>> activar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            simulacroService.cambiarActivo(escuelaDe(jwt), codigo, true), "Simulacro activado correctamente"));
    }

    // * Un Administrador siempre trae la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private static UUID escuelaDe(Jwt jwt) {
        String escuela = jwt == null ? null : jwt.getClaimAsString("escuela");
        try {
            return UUID.fromString(escuela);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    // * `sub` es el codigo_usuario de Identidad: la persona que crea el simulacro
    private static UUID autorDe(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu sesión no tiene una persona válida");
        }
    }
}
