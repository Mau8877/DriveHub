package com.drivehub.academico.features.reservas.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.academico.common.response.ApiResponse;
import com.drivehub.academico.common.response.PageMeta;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.CurrentEscuelaId;
import com.drivehub.academico.config.CurrentUserId;
import com.drivehub.academico.features.reservas.dto.CancelacionInstructorResponse;
import com.drivehub.academico.features.reservas.dto.ClaseAgendaResponse;
import com.drivehub.academico.features.reservas.service.AgendaService;

import lombok.RequiredArgsConstructor;

/**
 * La agenda del instructor (CU16) y la cancelación de una de sus clases. La decisión por rol vive en el service
 * (@PreAuthorize), no acá.
 *
 * * El instructor y su escuela salen del token, nunca de la URL ni del cuerpo.
 * * Comparte el prefijo `/api/v1/reservas` con las rutas del estudiante, pero son rutas distintas (`/agenda` y
 * * `/{codigo}/cancelar-por-instructor`) para no mezclar los roles en una misma ruta. Una clase se identifica por el
 * * código de su reserva.
 */
@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService service;
    private final BearerActual bearer;

    /** `filter[fecha]` (`yyyy-MM-dd`, hoy por defecto): las clases del instructor ese día, por hora. */
    @GetMapping("/agenda")
    public ResponseEntity<ApiResponse<List<ClaseAgendaResponse>>> agenda(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @RequestParam(name = "filter[fecha]", required = false) String fecha,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<ClaseAgendaResponse> pagina = service.agenda(instructorCodigo, escuelaCodigo, bearer.cabecera(), fecha,
            page, pageSize);
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Agenda obtenida correctamente",
            new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages())));
    }

    /** Cancela una clase del instructor: al estudiante se le devuelve siempre la hora y la franja queda bloqueada. */
    @PostMapping("/{codigo}/cancelar-por-instructor")
    public ResponseEntity<ApiResponse<CancelacionInstructorResponse>> cancelarPorInstructor(
            @CurrentUserId UUID instructorCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            @PathVariable("codigo") UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(
            service.cancelar(instructorCodigo, escuelaCodigo, bearer.cabecera(), codigo),
            "Clase cancelada correctamente"));
    }
}
