package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.CrearInstructorInput;
import com.drivehub.escuela.application.dto.EditarInstructorInput;
import com.drivehub.escuela.application.dto.InstructorResumenOutput;
import com.drivehub.escuela.application.dto.ListarInstructoresInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.CrearInstructorUseCase;
import com.drivehub.escuela.application.usecase.EditarInstructorUseCase;
import com.drivehub.escuela.application.usecase.ListarInstructoresUseCase;
import com.drivehub.escuela.application.usecase.ObtenerInstructorUseCase;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;
import com.drivehub.escuela.domain.entity.EstadoInstructor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de instructores (CU09): solo el Administrador de escuela, y solo los de su escuela.
 *
 * * La escuela sale SIEMPRE del token (`@EscuelaActual`), nunca de la URL ni del cuerpo.
 * * La decisión por rol vive en cada caso de uso (@PreAuthorize), no acá.
 * * La web no sabe que los datos de la persona viven en Identidad: pide todo a este recurso.
 */
@RestController
@RequestMapping("/api/v1/instructores")
@RequiredArgsConstructor
public class InstructorController {

    private final ListarInstructoresUseCase listarInstructores;
    private final ObtenerInstructorUseCase obtenerInstructor;
    private final CrearInstructorUseCase crearInstructor;
    private final EditarInstructorUseCase editarInstructor;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InstructorResumenResponse>>> listar(
            @EscuelaActual UUID escuelaCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) EstadoInstructor estado) {
        PaginaOutput<InstructorResumenOutput> pagina = listarInstructores.ejecutar(
            new ListarInstructoresInput(escuelaCodigo, page, pageSize, search, estado));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(InstructorResumenResponse::de).toList(),
            "Instructores obtenidos correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<InstructorDetalleResponse>> obtener(
            @EscuelaActual UUID escuelaCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            InstructorDetalleResponse.de(obtenerInstructor.ejecutar(escuelaCodigo, codigo)),
            "Instructor obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InstructorDetalleResponse>> crear(
            @EscuelaActual UUID escuelaCodigo, @Valid @RequestBody CrearInstructorRequest request) {
        InstructorDetalleResponse creado = InstructorDetalleResponse.de(crearInstructor.ejecutar(
            new CrearInstructorInput(escuelaCodigo, request.nombres(), request.apellidos(),
                request.fechaNacimiento(), request.sexo(), request.ci(), request.telefono(), request.email(),
                request.password(), request.latitud(), request.longitud(), request.nroLicencia(),
                request.licenciaVence(), request.fechaContratacion(), request.categorias())));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creado, "Instructor creado correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<InstructorDetalleResponse>> editar(
            @EscuelaActual UUID escuelaCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody EditarInstructorRequest request) {
        InstructorDetalleResponse editado = InstructorDetalleResponse.de(editarInstructor.ejecutar(
            new EditarInstructorInput(escuelaCodigo, codigo, request.nroLicencia(), request.licenciaVence(),
                request.fechaContratacion(), request.categorias(), request.estado(), request.latitud(),
                request.longitud())));
        return ResponseEntity.ok(ApiResponse.success(editado, "Instructor actualizado correctamente"));
    }
}
