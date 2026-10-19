package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.EscuelaEstadoOutput;
import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorCodigoUseCase;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorSlugUseCase;
import com.drivehub.escuela.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * Consulta pública de escuelas (sin sesión): la usan el formulario de
 * registro del estudiante (CU01) e Identidad para validar la escuela.
 */
@RestController
@RequestMapping("/api/v1/escuelas")
@RequiredArgsConstructor
public class EscuelaController {

    private final ObtenerEscuelaPorSlugUseCase obtenerEscuelaPorSlug;
    private final ObtenerEscuelaPorCodigoUseCase obtenerEscuelaPorCodigo;

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<EscuelaPublicaResponse>> obtenerPorSlug(@PathVariable String slug) {
        EscuelaPublicaOutput escuela = obtenerEscuelaPorSlug.ejecutar(slug);
        EscuelaPublicaResponse response =
            new EscuelaPublicaResponse(escuela.codigo(), escuela.slug(), escuela.nombre());
        return ResponseEntity.ok(ApiResponse.success(response, "Escuela obtenida correctamente"));
    }

    /**
     * Estado de una escuela por su codigo (activa o no). Lo llama Identidad por
     * la red interna al iniciar y renovar sesión (CU02); sin token a propósito.
     */
    @GetMapping("/por-codigo/{codigo}")
    public ResponseEntity<ApiResponse<EscuelaEstadoResponse>> obtenerPorCodigo(@PathVariable UUID codigo) {
        EscuelaEstadoOutput escuela = obtenerEscuelaPorCodigo.ejecutar(codigo);
        EscuelaEstadoResponse response = new EscuelaEstadoResponse(
            escuela.codigo(), escuela.slug(), escuela.nombre(), escuela.activa());
        return ResponseEntity.ok(ApiResponse.success(response, "Escuela obtenida correctamente"));
    }
}
