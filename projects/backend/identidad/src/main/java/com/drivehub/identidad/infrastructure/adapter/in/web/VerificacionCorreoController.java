package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.ConfirmarCorreoUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarVerificacionCorreoUseCase;
import com.drivehub.identidad.infrastructure.config.CurrentUserId;

import jakarta.validation.Valid;

/**
 * Verificación del correo (CU04b).
 *
 * * Pedir el correo exige sesión (JWT): se envía a la dirección de la cuenta.
 * * Confirmar es público: el enlace se abre fuera de la sesión, en cualquier navegador.
 */
@RestController
@RequestMapping("/api/v1/auth/verificar-correo")
public class VerificacionCorreoController {

    private final SolicitarVerificacionCorreoUseCase solicitarVerificacion;
    private final ConfirmarCorreoUseCase confirmarCorreo;

    public VerificacionCorreoController(
            SolicitarVerificacionCorreoUseCase solicitarVerificacion,
            ConfirmarCorreoUseCase confirmarCorreo) {
        this.solicitarVerificacion = solicitarVerificacion;
        this.confirmarCorreo = confirmarCorreo;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> solicitar(@CurrentUserId UUID usuarioCodigo) {
        solicitarVerificacion.solicitar(usuarioCodigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Te enviamos un correo de verificación"));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<ApiResponse<Void>> confirmar(@Valid @RequestBody ConfirmarCorreoRequest request) {
        confirmarCorreo.confirmar(request.token());
        return ResponseEntity.ok(ApiResponse.success(null, "Correo verificado correctamente"));
    }
}
