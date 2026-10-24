package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.ConfirmarCambioCorreoUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Confirmar el cambio de correo (CU06). Es público: el enlace se abre fuera de la sesión, en
 * cualquier navegador. Pedir el cambio exige sesión y vive en PerfilController.
 */
@RestController
@RequestMapping("/api/v1/auth/cambiar-correo")
@RequiredArgsConstructor
public class CambioCorreoController {

    private final ConfirmarCambioCorreoUseCase confirmarCambioCorreo;

    @PostMapping("/confirmar")
    public ResponseEntity<ApiResponse<Void>> confirmar(@Valid @RequestBody ConfirmarCambioCorreoRequest request) {
        confirmarCambioCorreo.confirmar(request.token());
        return ResponseEntity.ok(ApiResponse.success(null, "Correo cambiado correctamente"));
    }
}
