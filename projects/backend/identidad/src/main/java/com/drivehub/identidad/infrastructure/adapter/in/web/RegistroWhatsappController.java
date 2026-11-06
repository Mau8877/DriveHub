package com.drivehub.identidad.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.port.in.CompletarRegistroWhatsappUseCase;

import jakarta.validation.Valid;

/**
 * El registro por WhatsApp (CU48), la parte que usa la persona desde la web: elegir la contraseña. Es pública
 * (no hay sesión todavía): la autoriza el enlace de un solo uso, y el rate limit lo pone el gateway.
 */
@RestController
@RequestMapping("/api/v1/registros-whatsapp")
public class RegistroWhatsappController {

    private final CompletarRegistroWhatsappUseCase completarRegistro;

    public RegistroWhatsappController(CompletarRegistroWhatsappUseCase completarRegistro) {
        this.completarRegistro = completarRegistro;
    }

    @PostMapping("/completar")
    public ResponseEntity<ApiResponse<VinculoWhatsappResponse>> completar(
            @Valid @RequestBody CompletarRegistroRequest request) {
        boolean vinculado = completarRegistro.completar(request.token(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(new VinculoWhatsappResponse(vinculado), "Tu cuenta está lista"));
    }
}
