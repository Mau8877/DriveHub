package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ConfirmarMiAlumnoUseCase;
import com.drivehub.escuela.application.usecase.ListarMisAlumnosUseCase;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;

import lombok.RequiredArgsConstructor;

/**
 * Los alumnos del instructor (CU29): solo él, y solo los suyos. Las asignaciones las crea CU10.
 *
 * * El instructor sale SIEMPRE del token (`@UsuarioActual`) y la escuela del claim (`@EscuelaActual`).
 * * La decisión por rol vive en cada caso de uso (@PreAuthorize), no acá.
 */
@RestController
@RequestMapping("/api/v1/asignaciones-alumno/mis-alumnos")
@RequiredArgsConstructor
public class MisAlumnosController {

    private final ListarMisAlumnosUseCase listarMisAlumnos;
    private final ConfirmarMiAlumnoUseCase confirmarMiAlumno;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MiAlumnoResponse>>> listar(
            @EscuelaActual UUID escuelaCodigo,
            @UsuarioActual UUID instructorCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PaginaOutput<UUID> pagina = listarMisAlumnos.ejecutar(escuelaCodigo, instructorCodigo, page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(MiAlumnoResponse::new).toList(),
            "Alumnos obtenidos correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @GetMapping("/{estudianteCodigo}")
    public ResponseEntity<ApiResponse<MiAlumnoResponse>> confirmar(
            @EscuelaActual UUID escuelaCodigo,
            @UsuarioActual UUID instructorCodigo,
            @PathVariable UUID estudianteCodigo) {
        UUID codigo = confirmarMiAlumno.ejecutar(escuelaCodigo, instructorCodigo, estudianteCodigo);
        return ResponseEntity.ok(ApiResponse.success(new MiAlumnoResponse(codigo), "Alumno obtenido correctamente"));
    }
}
