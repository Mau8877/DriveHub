package com.drivehub.escuela.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
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

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<EscuelaPublicaResponse>> obtenerPorSlug(@PathVariable String slug) {
        EscuelaPublicaOutput escuela = obtenerEscuelaPorSlug.ejecutar(slug);
        EscuelaPublicaResponse response =
            new EscuelaPublicaResponse(escuela.codigo(), escuela.slug(), escuela.nombre());
        return ResponseEntity.ok(ApiResponse.success(response, "Escuela obtenida correctamente"));
    }
}
