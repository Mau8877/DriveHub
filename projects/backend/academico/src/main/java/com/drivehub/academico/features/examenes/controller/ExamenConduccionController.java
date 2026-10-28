package com.drivehub.academico.features.examenes.controller;

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
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.features.examenes.dto.ExamenResponse;
import com.drivehub.academico.features.examenes.dto.ProgramarExamenRequest;
import com.drivehub.academico.features.examenes.dto.RegistrarResultadoExamenRequest;
import com.drivehub.academico.features.examenes.dto.ReprogramarExamenRequest;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.repository.FiltroExamenes;
import com.drivehub.academico.features.examenes.service.ExamenConduccionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de los exámenes de conducción (CU22): solo el Administrador de escuela. La decisión por rol vive en el
 * service (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL.
 */
@RestController
@RequestMapping("/api/v1/examenes-conduccion")
@RequiredArgsConstructor
public class ExamenConduccionController {

    private final ExamenConduccionService examenService;
    private final BearerActual bearer;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamenResponse>> programar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProgramarExamenRequest request) {
        ExamenResponse creado = examenService.programar(escuelaDe(jwt), autorDe(jwt), bearer.cabecera(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creado, "Examen programado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamenResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[categoria]", required = false) String categoria,
            @RequestParam(name = "filter[estado]", required = false) EstadoExamen estado,
            @RequestParam(name = "filter[estudianteCodigo]", required = false) UUID estudianteCodigo) {
        Page<ExamenResponse> pagina = examenService.listar(escuelaDe(jwt), bearer.cabecera(),
            new FiltroExamenes(categoria, estado, estudianteCodigo), page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Exámenes obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ExamenResponse>> obtener(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            examenService.obtener(escuelaDe(jwt), bearer.cabecera(), codigo), "Examen obtenido correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ExamenResponse>> reprogramar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo,
            @Valid @RequestBody ReprogramarExamenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            examenService.reprogramar(escuelaDe(jwt), codigo, request), "Examen reprogramado correctamente"));
    }

    @PostMapping("/{codigo}/cancelar")
    public ResponseEntity<ApiResponse<ExamenResponse>> cancelar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            examenService.cancelar(escuelaDe(jwt), codigo), "Examen cancelado correctamente"));
    }

    @PostMapping("/{codigo}/no-asistio")
    public ResponseEntity<ApiResponse<ExamenResponse>> noAsistio(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            examenService.marcarNoAsistio(escuelaDe(jwt), codigo), "Inasistencia registrada correctamente"));
    }

    @PostMapping("/{codigo}/registrar-resultado")
    public ResponseEntity<ApiResponse<ExamenResponse>> registrarResultado(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo,
            @Valid @RequestBody RegistrarResultadoExamenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            examenService.registrarResultado(escuelaDe(jwt), codigo, request), "Resultado registrado correctamente"));
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

    // * `sub` es el codigo_usuario de Identidad: la persona que programa el examen
    private static UUID autorDe(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu sesión no tiene una persona válida");
        }
    }
}
