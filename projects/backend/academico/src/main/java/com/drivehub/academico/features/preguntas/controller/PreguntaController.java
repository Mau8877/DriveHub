package com.drivehub.academico.features.preguntas.controller;

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
import com.drivehub.academico.features.preguntas.dto.CrearPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.EditarPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.PreguntaResponse;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.repository.FiltroPreguntas;
import com.drivehub.academico.features.preguntas.service.PreguntaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión del banco de preguntas (CU19): solo el Administrador de escuela. La decisión por rol vive
 * en el service (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL: así un
 * * Administrador no puede tocar el banco de otra escuela.
 */
@RestController
@RequestMapping("/api/v1/preguntas")
@RequiredArgsConstructor
public class PreguntaController {

    private final PreguntaService preguntaService;

    @PostMapping
    public ResponseEntity<ApiResponse<PreguntaResponse>> crear(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearPreguntaRequest request) {
        PreguntaResponse creada = preguntaService.crear(escuelaDe(jwt), autorDe(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creada, "Pregunta creada correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PreguntaResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[categoria]", required = false) String categoria,
            @RequestParam(name = "filter[tema]", required = false) String tema,
            @RequestParam(name = "filter[tipo]", required = false) TipoPregunta tipo,
            @RequestParam(name = "filter[dificultad]", required = false) Integer dificultad,
            @RequestParam(name = "filter[activa]", required = false) Boolean activa) {
        Page<PreguntaResponse> pagina = preguntaService.listar(escuelaDe(jwt),
            new FiltroPreguntas(search, categoria, tema, tipo, dificultad, activa), page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Preguntas obtenidas correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<PreguntaResponse>> obtener(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            preguntaService.obtener(escuelaDe(jwt), codigo), "Pregunta obtenida correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<PreguntaResponse>> editar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo,
            @Valid @RequestBody EditarPreguntaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            preguntaService.editar(escuelaDe(jwt), codigo, request), "Pregunta actualizada correctamente"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<PreguntaResponse>> desactivar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            preguntaService.cambiarActiva(escuelaDe(jwt), codigo, false), "Pregunta desactivada correctamente"));
    }

    @PostMapping("/{codigo}/activar")
    public ResponseEntity<ApiResponse<PreguntaResponse>> activar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            preguntaService.cambiarActiva(escuelaDe(jwt), codigo, true), "Pregunta activada correctamente"));
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

    // * `sub` es el codigo_usuario de Identidad: la persona que crea la pregunta
    private static UUID autorDe(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu sesión no tiene una persona válida");
        }
    }
}
