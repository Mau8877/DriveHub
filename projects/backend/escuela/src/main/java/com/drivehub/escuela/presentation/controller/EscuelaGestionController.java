package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.CrearEscuelaInput;
import com.drivehub.escuela.application.dto.EscuelaCreadaOutput;
import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.application.dto.ListarEscuelasInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.CrearEscuelaUseCase;
import com.drivehub.escuela.application.usecase.DesactivarEscuelaUseCase;
import com.drivehub.escuela.application.usecase.EditarEscuelaUseCase;
import com.drivehub.escuela.application.usecase.ListarEscuelasUseCase;
import com.drivehub.escuela.application.usecase.ReactivarEscuelaUseCase;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.gateway.DatosAdministrador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de escuelas (CU05): solo el Super admin. La decisión por rol vive en cada caso de uso
 * (@PreAuthorize), no acá. La consulta pública por slug y por código es la de EscuelaController.
 */
@RestController
@RequestMapping("/api/v1/escuelas")
@RequiredArgsConstructor
public class EscuelaGestionController {

    private final CrearEscuelaUseCase crearEscuela;
    private final ListarEscuelasUseCase listarEscuelas;
    private final EditarEscuelaUseCase editarEscuela;
    private final DesactivarEscuelaUseCase desactivarEscuela;
    private final ReactivarEscuelaUseCase reactivarEscuela;

    @PostMapping
    public ResponseEntity<ApiResponse<EscuelaCreadaResponse>> crear(@Valid @RequestBody CrearEscuelaRequest request) {
        CrearEscuelaRequest.AdministradorRequest admin = request.administrador();
        EscuelaCreadaOutput creada = crearEscuela.ejecutar(new CrearEscuelaInput(request.nombre(), request.slug(),
            new DatosAdministrador(admin.nombres(), admin.apellidos(), admin.fechaNacimiento(), admin.email(),
                admin.password(), admin.telefono())));
        EscuelaCreadaResponse response = new EscuelaCreadaResponse(creada.codigo(), creada.slug(), creada.nombre(),
            creada.estado(), creada.creadoEn(), new EscuelaCreadaResponse.AdministradorResponse(
                creada.administrador().codigo(), creada.administrador().email(),
                creada.administrador().nombres(), creada.administrador().apellidos()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Escuela creada correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EscuelaResponse>>> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) EstadoEscuela estado) {
        PaginaOutput<EscuelaOutput> pagina = listarEscuelas.ejecutar(new ListarEscuelasInput(page, pageSize, search, estado));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(EscuelaGestionController::aRespuesta).toList(),
            "Escuelas obtenidas correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<EscuelaResponse>> editar(
            @PathVariable UUID codigo, @Valid @RequestBody EditarEscuelaRequest request) {
        EscuelaOutput editada = editarEscuela.ejecutar(codigo, request.nombre());
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(editada), "Escuela actualizada correctamente"));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID codigo) {
        desactivarEscuela.ejecutar(codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Escuela desactivada correctamente"));
    }

    @PostMapping("/{codigo}/reactivar")
    public ResponseEntity<ApiResponse<EscuelaResponse>> reactivar(@PathVariable UUID codigo) {
        EscuelaOutput reactivada = reactivarEscuela.ejecutar(codigo);
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(reactivada), "Escuela reactivada correctamente"));
    }

    private static EscuelaResponse aRespuesta(EscuelaOutput escuela) {
        return new EscuelaResponse(escuela.codigo(), escuela.slug(), escuela.nombre(), escuela.estado(), escuela.creadoEn());
    }
}
