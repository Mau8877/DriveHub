package com.drivehub.pagos.features.comprobantes.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.common.util.EscuelaDelToken;
import com.drivehub.pagos.config.CurrentEscuelaId;
import com.drivehub.pagos.config.CurrentUserId;
import com.drivehub.pagos.features.comprobantes.service.ComprobanteDescargaService;

import lombok.RequiredArgsConstructor;

/**
 * El PDF de un comprobante (CU30). La decisión por rol y por dueño vive en el service, no acá.
 *
 * * Es una EXCEPCIÓN al sobre `ApiResponse`: responde un archivo (los bytes del PDF). Los errores sí usan el sobre.
 * * `Cache-Control: no-store`: el comprobante lleva datos personales y no se guarda en cachés.
 */
@RestController
@RequestMapping("/api/v1/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private static final String ROL_INSTRUCTOR = "ROLE_INSTRUCTOR";

    private final ComprobanteDescargaService descargaService;

    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> pdf(
            @CurrentUserId UUID personaCodigo,
            @CurrentEscuelaId UUID escuelaCodigo,
            Authentication autenticacion,
            @PathVariable UUID codigo) {
        boolean esInstructor = autenticacion.getAuthorities().stream()
            .anyMatch(a -> ROL_INSTRUCTOR.equals(a.getAuthority()));
        ComprobanteDescargaService.Pdf pdf = descargaService.obtener(EscuelaDelToken.exigir(escuelaCodigo),
            personaCodigo, esInstructor, codigo);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(pdf.numero() + ".pdf").build().toString())
            .cacheControl(CacheControl.noStore())
            .body(pdf.bytes());
    }
}
