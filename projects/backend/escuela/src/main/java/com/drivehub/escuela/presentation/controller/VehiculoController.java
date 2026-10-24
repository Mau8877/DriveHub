package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.EditarVehiculoInput;
import com.drivehub.escuela.application.dto.ListarVehiculosInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.dto.RegistrarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.application.usecase.CambiarEstadoVehiculoUseCase;
import com.drivehub.escuela.application.usecase.EditarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ListarVehiculosUseCase;
import com.drivehub.escuela.application.usecase.ObtenerVehiculoUseCase;
import com.drivehub.escuela.application.usecase.RegistrarVehiculoUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ForbiddenException;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de autos (CU07): solo el Administrador de escuela. La decisión por rol vive en cada
 * caso de uso (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca del cuerpo ni de la URL: así un
 * * Administrador no puede tocar los autos de otra escuela.
 */
@RestController
@RequestMapping("/api/v1/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final RegistrarVehiculoUseCase registrarVehiculo;
    private final ListarVehiculosUseCase listarVehiculos;
    private final ObtenerVehiculoUseCase obtenerVehiculo;
    private final EditarVehiculoUseCase editarVehiculo;
    private final CambiarEstadoVehiculoUseCase cambiarEstado;

    @PostMapping
    public ResponseEntity<ApiResponse<VehiculoResponse>> registrar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearVehiculoRequest request) {
        VehiculoOutput creado = registrarVehiculo.ejecutar(new RegistrarVehiculoInput(escuelaDe(jwt), request.placa(),
            request.marca(), request.modelo(), request.anio(), request.color(), request.transmision(),
            request.kilometraje(), request.soatVence(), request.inspeccionVence()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(aRespuesta(creado), "Auto registrado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehiculoResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) EstadoVehiculo estado) {
        PaginaOutput<VehiculoOutput> pagina = listarVehiculos.ejecutar(
            new ListarVehiculosInput(escuelaDe(jwt), page, pageSize, search, estado));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(VehiculoController::aRespuesta).toList(),
            "Autos obtenidos correctamente",
            new PageMeta(pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas())));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<VehiculoResponse>> obtener(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            aRespuesta(obtenerVehiculo.ejecutar(escuelaDe(jwt), codigo)), "Auto obtenido correctamente"));
    }

    @PatchMapping("/{codigo}")
    public ResponseEntity<ApiResponse<VehiculoResponse>> editar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo,
            @Valid @RequestBody EditarVehiculoRequest request) {
        VehiculoOutput editado = editarVehiculo.ejecutar(new EditarVehiculoInput(escuelaDe(jwt), codigo,
            request.marca(), request.modelo(), request.anio(), request.color(), request.kilometraje(),
            request.soatVence(), request.inspeccionVence()));
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(editado), "Auto actualizado correctamente"));
    }

    @PostMapping("/{codigo}/enviar-a-mantenimiento")
    public ResponseEntity<ApiResponse<VehiculoResponse>> enviarAMantenimiento(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return cambiar(jwt, codigo, EstadoVehiculo.MANTENIMIENTO, "Auto enviado a mantenimiento");
    }

    @PostMapping("/{codigo}/habilitar")
    public ResponseEntity<ApiResponse<VehiculoResponse>> habilitar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return cambiar(jwt, codigo, EstadoVehiculo.DISPONIBLE, "Auto habilitado correctamente");
    }

    @PostMapping("/{codigo}/dar-de-baja")
    public ResponseEntity<ApiResponse<VehiculoResponse>> darDeBaja(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID codigo) {
        return cambiar(jwt, codigo, EstadoVehiculo.BAJA, "Auto dado de baja correctamente");
    }

    private ResponseEntity<ApiResponse<VehiculoResponse>> cambiar(
            Jwt jwt, UUID codigo, EstadoVehiculo destino, String mensaje) {
        VehiculoOutput cambiado = cambiarEstado.ejecutar(escuelaDe(jwt), codigo, destino);
        return ResponseEntity.ok(ApiResponse.success(aRespuesta(cambiado), mensaje));
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

    private static VehiculoResponse aRespuesta(VehiculoOutput v) {
        return new VehiculoResponse(v.codigo(), v.placa(), v.marca(), v.modelo(), v.anio(), v.color(),
            v.transmision(), v.estado(), v.kilometraje(), v.soatVence(), v.inspeccionVence(), v.creadoEn());
    }
}
