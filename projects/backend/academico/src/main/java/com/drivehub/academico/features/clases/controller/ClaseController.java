package com.drivehub.academico.features.clases.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.dto.CerrarClaseRequest;
import com.drivehub.academico.features.clases.dto.ClaseResponse;
import com.drivehub.academico.features.clases.service.ClaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las clases dictadas del instructor (CU17): cerrar una clase que ya empezó, verla y corregirla. La decisión por rol
 * vive en el service (@PreAuthorize), no acá.
 *
 * * El instructor y su escuela salen del token, nunca de la URL ni del cuerpo.
 * * Cerrar es `POST /clases` con el código de la reserva en el cuerpo; el `{codigo}` de las otras dos rutas es el de
 * * la clase dictada (la agenda lo entrega como `claseCodigo`).
 */
@RestController
@RequestMapping("/api/v1/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final ClaseService service;
    private final BearerActual bearer;

    @PostMapping
    public ResponseEntity<ApiResponse<ClaseResponse>> cerrar(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @Valid @RequestBody CerrarClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            service.cerrar(instructorCodigo, escuelaCodigo, bearer.cabecera(), request),
            "Clase cerrada correctamente"));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ClaseResponse>> obtener(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable("codigo") UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            service.obtener(instructorCodigo, escuelaCodigo, bearer.cabecera(), codigo),
            "Clase obtenida correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<ClaseResponse>> actualizar(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable("codigo") UUID codigo,
            @Valid @RequestBody ActualizarClaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            service.actualizar(instructorCodigo, escuelaCodigo, bearer.cabecera(), codigo, request),
            "Clase actualizada correctamente"));
    }
}
