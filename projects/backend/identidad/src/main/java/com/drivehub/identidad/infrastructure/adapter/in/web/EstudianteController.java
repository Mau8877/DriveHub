package com.drivehub.identidad.infrastructure.adapter.in.web;

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

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.common.response.PageMeta;
import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;
import com.drivehub.identidad.domain.port.in.CrearEstudianteComando;
import com.drivehub.identidad.domain.port.in.CrearEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.EditarEstudianteComando;
import com.drivehub.identidad.domain.port.in.EditarEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesComando;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesUseCase;
import com.drivehub.identidad.domain.port.in.ObtenerEstudianteUseCase;
import com.drivehub.identidad.domain.port.in.PaginaEstudiantes;
import com.drivehub.identidad.infrastructure.config.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de estudiantes (CU08): solo el Administrador de escuela, y solo los de su escuela.
 *
 * * El Administrador sale SIEMPRE de la sesión (`@CurrentUserId`), nunca de la URL ni del cuerpo; de
 * * su cuenta se toma la escuela.
 * * La decisión por rol vive en cada caso de uso (@PreAuthorize), no acá.
 */
@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final ListarEstudiantesUseCase listarEstudiantes;
    private final ObtenerEstudianteUseCase obtenerEstudiante;
    private final CrearEstudianteUseCase crearEstudiante;
    private final EditarEstudianteUseCase editarEstudiante;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EstudianteResumenResponse>>> listar(
            @CurrentUserId UUID administradorCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) EstadoFiltroEstudiante estado) {
        PaginaEstudiantes pagina = listarEstudiantes.listar(
            new ListarEstudiantesComando(administradorCodigo, page, pageSize, search, estado));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(EstudianteResumenResponse::de).toList(),
            "Estudiantes obtenidos correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<EstudianteDetalleResponse>> obtener(
            @CurrentUserId UUID administradorCodigo, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            EstudianteDetalleResponse.de(obtenerEstudiante.obtener(administradorCodigo, codigo)),
            "Estudiante obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EstudianteDetalleResponse>> crear(
            @CurrentUserId UUID administradorCodigo, @Valid @RequestBody CrearEstudianteRequest request) {
        EstudianteDetalleResponse creado = EstudianteDetalleResponse.de(crearEstudiante.crear(
            new CrearEstudianteComando(administradorCodigo, request.nombres(), request.apellidos(),
                request.fechaNacimiento(), request.sexo(), request.ci(), request.telefono(), request.email(),
                request.password(), request.latitud(), request.longitud(), request.categoriaSigla())));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creado, "Estudiante creado correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<EstudianteDetalleResponse>> editar(
            @CurrentUserId UUID administradorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody EditarEstudianteRequest request) {
        EstudianteDetalleResponse editado = EstudianteDetalleResponse.de(editarEstudiante.editar(
            new EditarEstudianteComando(administradorCodigo, codigo, request.categoriaSigla(), request.latitud(),
                request.longitud())));
        return ResponseEntity.ok(ApiResponse.success(editado, "Estudiante actualizado correctamente"));
    }
}
