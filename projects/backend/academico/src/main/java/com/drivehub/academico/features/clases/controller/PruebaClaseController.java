package com.drivehub.academico.features.clases.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.clases.dto.CatalogoPruebasResponse;
import com.drivehub.academico.features.clases.dto.EvaluarClaseRequest;
import com.drivehub.academico.features.clases.dto.PruebaClaseResponse;
import com.drivehub.academico.features.clases.service.PruebaClaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las pruebas de clase del instructor (CU18): evaluar una clase ya cerrada, ver sus pruebas y pedir los criterios. La
 * decisión por rol vive en el service (@PreAuthorize), no acá.
 *
 * * El instructor y su escuela salen del token, nunca de la URL ni del cuerpo.
 * * Las pruebas de una clase se listan con `filter[claseCodigo]` (obligatorio): no hay un listado general.
 */
@RestController
@RequestMapping("/api/v1/pruebas-clase")
@RequiredArgsConstructor
public class PruebaClaseController {

    private final PruebaClaseService service;

    @PostMapping
    public ResponseEntity<ApiResponse<PruebaClaseResponse>> evaluar(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @Valid @RequestBody EvaluarClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            service.evaluar(instructorCodigo, escuelaCodigo, request), "Prueba de clase registrada correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PruebaClaseResponse>>> listar(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(name = "filter[claseCodigo]") UUID claseCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Page<PruebaClaseResponse> pagina = service.listarPorClase(instructorCodigo, escuelaCodigo, claseCodigo, page,
            pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Listado obtenido correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    @GetMapping("/criterios")
    public ResponseEntity<ApiResponse<CatalogoPruebasResponse>> criterios() {
        return ResponseEntity.ok(ApiResponse.success(service.criterios(), "Criterios obtenidos correctamente"));
    }
}
