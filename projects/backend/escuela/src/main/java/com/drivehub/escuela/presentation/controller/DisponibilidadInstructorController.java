package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.escuela.application.dto.ConsultarDisponibilidadInput;
import com.drivehub.escuela.application.dto.DisponibilidadInstructorOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ConsultarDisponibilidadUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ForbiddenException;
import com.drivehub.escuela.common.response.ApiResponse;
import com.drivehub.escuela.common.response.PageMeta;
import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.MomentoDelDia;

import lombok.RequiredArgsConstructor;

/**
 * Disponibilidad de los instructores de la escuela (CU11): solo el Administrador de escuela, y solo lectura. La
 * decisión por rol vive en el caso de uso (@PreAuthorize), no acá.
 *
 * * La escuela sale del claim `escuela` del token, nunca de un parámetro.
 * * `filter[fecha]` es `yyyy-MM-dd` (hoy por defecto); un valor que no es una fecha, o un filtro con un valor
 * * desconocido, responde 400 `ERR_SYS_01`.
 */
@RestController
@RequestMapping("/api/v1/disponibilidad-instructores")
@RequiredArgsConstructor
public class DisponibilidadInstructorController {

    private final ConsultarDisponibilidadUseCase consultarDisponibilidad;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DisponibilidadInstructorResponse>>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[fecha]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "filter[momento]", required = false) MomentoDelDia momento,
            @RequestParam(name = "filter[disponibilidad]", required = false) Disponibilidad disponibilidad,
            @RequestParam(name = "filter[auto]", required = false) EstadoAutos estadoAutos) {
        PaginaOutput<DisponibilidadInstructorOutput> pagina = consultarDisponibilidad.ejecutar(
            new ConsultarDisponibilidadInput(escuelaDe(jwt), page, pageSize, search, fecha, momento, disponibilidad,
                estadoAutos));
        return ResponseEntity.ok(ApiResponse.successList(
            pagina.elementos().stream().map(DisponibilidadInstructorController::aRespuesta).toList(),
            "Disponibilidad obtenida correctamente",
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

    private static DisponibilidadInstructorResponse aRespuesta(DisponibilidadInstructorOutput o) {
        return new DisponibilidadInstructorResponse(o.instructorCodigo(), o.nombreCompleto(), o.estadoInstructor(),
            o.disponibilidad(), o.estadoAutos(),
            o.autos().stream().map(a -> new AutoDeInstructorResponse(a.vehiculoCodigo(), a.placa(), a.marca(),
                a.modelo(), a.estado())).toList(),
            o.libres(), o.reservadas(), o.bloqueadas());
    }
}
