package com.drivehub.identidad.infrastructure.adapter.in.web;

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

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.common.response.PageMeta;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresComando;
import com.drivehub.identidad.domain.port.in.BuscarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.ConsultarInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.ConsultarNombresInstructoresUseCase;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorComando;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorUseCase;
import com.drivehub.identidad.domain.port.in.EditarUbicacionInstructorUseCase;
import com.drivehub.identidad.domain.port.in.PaginaPersonasInstructor;
import com.drivehub.identidad.domain.port.in.RevertirCuentaInstructorUseCase;
import com.drivehub.identidad.infrastructure.config.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Cuentas de los instructores de una escuela (CU09). Las llama Escuela, con el token del
 * Administrador de escuela; el frontend usa `/api/v1/instructores` (Escuela). La excepción es `/nombres`
 * (CU14): lo llama Académico con el token de un estudiante o de un instructor.
 *
 * * El Administrador sale SIEMPRE de la sesión (`@CurrentUserId`), nunca de la URL ni del cuerpo; de
 * * su cuenta se toma la escuela.
 * * La decisión por rol vive en cada caso de uso (@PreAuthorize), no acá.
 */
@RestController
@RequestMapping("/api/v1/usuarios/instructores")
@RequiredArgsConstructor
public class InstructorCuentaController {

    private final CrearCuentaInstructorUseCase crearCuenta;
    private final ConsultarInstructoresUseCase consultarInstructores;
    private final ConsultarNombresInstructoresUseCase consultarNombres;
    private final BuscarInstructoresUseCase buscarInstructores;
    private final EditarUbicacionInstructorUseCase editarUbicacion;
    private final RevertirCuentaInstructorUseCase revertirCuenta;

    @PostMapping
    public ResponseEntity<ApiResponse<PersonaInstructorResponse>> crear(
            @CurrentUserId UUID administradorCodigo, @Valid @RequestBody CrearCuentaInstructorRequest request) {
        PersonaInstructorResponse creada = PersonaInstructorResponse.de(crearCuenta.crear(
            new CrearCuentaInstructorComando(administradorCodigo, request.codigo(), request.nombres(),
                request.apellidos(), request.fechaNacimiento(), request.sexo(), request.ci(), request.telefono(),
                request.email(), request.password(), request.latitud(), request.longitud())));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(creada, "Cuenta de instructor creada correctamente"));
    }

    @PostMapping("/consulta")
    public ResponseEntity<ApiResponse<List<PersonaInstructorResponse>>> consultar(
            @CurrentUserId UUID administradorCodigo, @Valid @RequestBody ConsultarInstructoresRequest request) {
        List<PersonaInstructorResponse> personas = consultarInstructores
            .consultar(administradorCodigo, request.codigos()).stream().map(PersonaInstructorResponse::de).toList();
        return ResponseEntity.ok(ApiResponse.success(personas, "Instructores obtenidos correctamente"));
    }

    /**
     * CU14: solo el nombre de los instructores de la escuela de quien pregunta (un estudiante o un
     * instructor). A diferencia de `/consulta`, no devuelve CI, correo ni teléfono.
     */
    @PostMapping("/nombres")
    public ResponseEntity<ApiResponse<List<NombreInstructorResponse>>> nombres(
            @CurrentUserId UUID solicitanteCodigo, @Valid @RequestBody ConsultarNombresInstructoresRequest request) {
        List<NombreInstructorResponse> nombres = consultarNombres
            .consultar(solicitanteCodigo, request.codigos()).stream().map(NombreInstructorResponse::de).toList();
        return ResponseEntity.ok(ApiResponse.success(nombres, "Instructores obtenidos correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonaInstructorResponse>>> buscar(
            @CurrentUserId UUID administradorCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search) {
        PaginaPersonasInstructor pagina = buscarInstructores.buscar(
            new BuscarInstructoresComando(administradorCodigo, page, pageSize, search));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(PersonaInstructorResponse::de).toList(),
            "Instructores obtenidos correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @PatchMapping("/{codigo}/ubicacion")
    public ResponseEntity<ApiResponse<PersonaInstructorResponse>> editarUbicacion(
            @CurrentUserId UUID administradorCodigo, @PathVariable UUID codigo,
            @Valid @RequestBody EditarUbicacionRequest request) {
        PersonaInstructorResponse editada = PersonaInstructorResponse.de(
            editarUbicacion.editar(administradorCodigo, codigo, request.latitud(), request.longitud()));
        return ResponseEntity.ok(ApiResponse.success(editada, "Ubicación actualizada correctamente"));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<ApiResponse<Void>> revertir(
            @CurrentUserId UUID administradorCodigo, @PathVariable UUID codigo) {
        revertirCuenta.revertir(administradorCodigo, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Cuenta de instructor revertida correctamente"));
    }
}
