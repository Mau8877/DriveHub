package com.drivehub.academico.features.simulacros.controller;

import java.util.List;
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

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.simulacros.dto.FinalizarIntentoRequest;
import com.drivehub.academico.features.simulacros.dto.IniciarIntentoRequest;
import com.drivehub.academico.features.simulacros.dto.IntentoIniciadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResultadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResumenResponse;
import com.drivehub.academico.features.simulacros.service.IntentoSimulacroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Rendir simulacros escritos (CU21): solo el estudiante. La decisión por rol vive en el service (@PreAuthorize).
 *
 * * El estudiante y su escuela salen del token, nunca del cuerpo ni de la URL: un intento solo se ve y se termina
 * * desde la cuenta que lo inició.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IntentoSimulacroController {

    private final IntentoSimulacroService intentoService;

    @GetMapping("/simulacros/disponibles")
    public ResponseEntity<ApiResponse<List<com.drivehub.academico.features.simulacros.dto.SimulacroDisponibleResponse>>>
            disponibles(@CurrentEscuelaId UUID escuelaCodigo) {
        return ResponseEntity.ok(ApiResponse.success(intentoService.disponibles(escuela(escuelaCodigo)),
            "Simulacros obtenidos correctamente"));
    }

    @PostMapping("/intentos-simulacro")
    public ResponseEntity<ApiResponse<IntentoIniciadoResponse>> iniciar(
            @CurrentUserId UUID estudianteCodigo, @CurrentEscuelaId UUID escuelaCodigo,
            @Valid @RequestBody IniciarIntentoRequest request) {
        IntentoIniciadoResponse iniciado =
            intentoService.iniciar(escuela(escuelaCodigo), estudianteCodigo, request.simulacroCodigo());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(iniciado, "Intento iniciado correctamente"));
    }

    @GetMapping("/intentos-simulacro")
    public ResponseEntity<ApiResponse<List<IntentoResumenResponse>>> listar(
            @CurrentUserId UUID estudianteCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Page<IntentoResumenResponse> pagina = intentoService.listarMios(estudianteCodigo, page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Intentos obtenidos correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/intentos-simulacro/{codigo}")
    public ResponseEntity<ApiResponse<IntentoResultadoResponse>> obtener(
            @CurrentUserId UUID estudianteCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            intentoService.obtener(estudianteCodigo, codigo), "Intento obtenido correctamente"));
    }

    @PostMapping("/intentos-simulacro/{codigo}/finalizar")
    public ResponseEntity<ApiResponse<IntentoResultadoResponse>> finalizar(
            @CurrentUserId UUID estudianteCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody FinalizarIntentoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            intentoService.finalizar(estudianteCodigo, codigo, request), "Intento finalizado correctamente"));
    }

    // * Un estudiante siempre trae la escuela en el token (CU02); sin ella no hay tenant del que ofrecer simulacros
    private static UUID escuela(UUID escuelaCodigo) {
        if (escuelaCodigo == null) {
            throw new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
        return escuelaCodigo;
    }
}
