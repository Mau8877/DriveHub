package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.CambiarAsignacionInput;
import com.drivehub.escuela.application.dto.HistorialAsignacionOutput;
import com.drivehub.escuela.application.dto.InstructorAsignadoOutput;
import com.drivehub.escuela.application.dto.ListarAsignacionesInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.AsignarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.CambiarInstructorVehiculoUseCase;
import com.drivehub.escuela.application.usecase.LiberarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ListarAsignacionesVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ObtenerHistorialVehiculoUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ForbiddenException;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Asignación de autos a instructores (CU10): solo el Administrador de escuela. La decisión por rol
 * vive en cada caso de uso (@PreAuthorize), no acá.
 *
 * * El recurso es "la asignación de cada auto": el identificador de la URL es el código del auto.
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL.
 */
@RestController
@RequestMapping("/api/v1/asignaciones-vehiculo")
@RequiredArgsConstructor
public class AsignacionVehiculoController {

    private final ListarAsignacionesVehiculoUseCase listarAsignaciones;
    private final AsignarVehiculoUseCase asignarVehiculo;
    private final CambiarInstructorVehiculoUseCase cambiarInstructor;
    private final LiberarVehiculoUseCase liberarVehiculo;
    private final ObtenerHistorialVehiculoUseCase obtenerHistorial;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AsignacionVehiculoResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[asignacion]", required = false) FiltroAsignaciones.Asignacion asignacion) {
        PaginaOutput<AsignacionVehiculoOutput> pagina = listarAsignaciones.ejecutar(
            new ListarAsignacionesInput(escuelaDe(jwt), page, pageSize, search, asignacion));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(AsignacionVehiculoController::aRespuesta).toList(),
            "Asignaciones obtenidas correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @PostMapping("/{vehiculoCodigo}/asignar")
    public ResponseEntity<ApiResponse<AsignacionVehiculoResponse>> asignar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID vehiculoCodigo,
            @Valid @RequestBody AsignacionInstructorRequest request) {
        AsignacionVehiculoOutput asignada = asignarVehiculo.ejecutar(new CambiarAsignacionInput(
            escuelaDe(jwt), vehiculoCodigo, request.instructorCodigo(), request.fecha()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(aRespuesta(asignada), "Auto asignado correctamente"));
    }

    @PostMapping("/{vehiculoCodigo}/cambiar-instructor")
    public ResponseEntity<ApiResponse<AsignacionVehiculoResponse>> cambiarInstructor(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID vehiculoCodigo,
            @Valid @RequestBody AsignacionInstructorRequest request) {
        AsignacionVehiculoOutput cambiada = cambiarInstructor.ejecutar(new CambiarAsignacionInput(
            escuelaDe(jwt), vehiculoCodigo, request.instructorCodigo(), request.fecha()));
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(cambiada), "Instructor cambiado correctamente"));
    }

    @PostMapping("/{vehiculoCodigo}/liberar")
    public ResponseEntity<ApiResponse<AsignacionVehiculoResponse>> liberar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID vehiculoCodigo,
            @RequestBody(required = false) LiberarVehiculoRequest request) {
        AsignacionVehiculoOutput liberada = liberarVehiculo.ejecutar(
            escuelaDe(jwt), vehiculoCodigo, request == null ? null : request.fecha());
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(liberada), "Auto liberado correctamente"));
    }

    @GetMapping("/{vehiculoCodigo}/historial")
    public ResponseEntity<ApiResponse<List<HistorialAsignacionResponse>>> historial(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID vehiculoCodigo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PaginaOutput<HistorialAsignacionOutput> pagina = obtenerHistorial.ejecutar(
            escuelaDe(jwt), vehiculoCodigo, page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(AsignacionVehiculoController::aRespuesta).toList(),
            "Historial obtenido correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    // * Un Administrador siempre trae la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private static UUID escuelaDe(Jwt jwt) {
        String escuela = jwt == null ? null : jwt.getClaimAsString("escuela");
        if (escuela == null) {
            throw new ForbiddenException(EscuelaErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
        try {
            return UUID.fromString(escuela);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException(EscuelaErrorCodes.TOKEN_SIN_ESCUELA, "Tu cuenta no pertenece a ninguna escuela");
        }
    }

    private static InstructorAsignadoResponse aRespuesta(InstructorAsignadoOutput i) {
        return i == null ? null : new InstructorAsignadoResponse(i.codigo(), i.nombreCompleto());
    }

    private static AsignacionVehiculoResponse aRespuesta(AsignacionVehiculoOutput a) {
        return new AsignacionVehiculoResponse(a.vehiculoCodigo(), a.placa(), a.marca(), a.modelo(),
            a.estadoVehiculo(), aRespuesta(a.instructor()), a.fechaInicio());
    }

    private static HistorialAsignacionResponse aRespuesta(HistorialAsignacionOutput h) {
        return new HistorialAsignacionResponse(h.codigo(), aRespuesta(h.instructor()), h.fechaInicio(), h.fechaFin());
    }
}
