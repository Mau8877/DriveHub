package com.drivehub.escuela.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.CategoriaLicenciaOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ListarCategoriasLicenciaUseCase;
import com.drivehub.escuela.application.usecase.ObtenerCategoriaLicenciaUseCase;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo de categorías de licencia (CU08): solo lectura y solo para el Administrador de escuela.
 * La decisión por rol vive en cada caso de uso (@PreAuthorize), no acá.
 */
@RestController
@RequestMapping("/api/v1/categorias-licencia")
@RequiredArgsConstructor
public class CategoriaLicenciaController {

    private final ListarCategoriasLicenciaUseCase listarCategorias;
    private final ObtenerCategoriaLicenciaUseCase obtenerCategoria;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaLicenciaResponse>>> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PaginaOutput<CategoriaLicenciaOutput> pagina = listarCategorias.ejecutar(page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(CategoriaLicenciaController::aRespuesta).toList(),
            "Categorías obtenidas correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @GetMapping("/{sigla}")
    public ResponseEntity<ApiResponse<CategoriaLicenciaResponse>> obtener(@PathVariable String sigla) {
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(obtenerCategoria.ejecutar(sigla)),
            "Categoría obtenida correctamente"));
    }

    private static CategoriaLicenciaResponse aRespuesta(CategoriaLicenciaOutput categoria) {
        return new CategoriaLicenciaResponse(categoria.sigla(), categoria.descripcion());
    }
}
