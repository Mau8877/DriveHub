package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.common.response.ApiResponse;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CerrarSesionComando;
import com.drivehub.identidad.domain.port.in.CerrarSesionUseCase;
import com.drivehub.identidad.domain.port.in.IniciarSesionComando;
import com.drivehub.identidad.domain.port.in.IniciarSesionUseCase;
import com.drivehub.identidad.domain.port.in.RenovarSesionComando;
import com.drivehub.identidad.domain.port.in.RenovarSesionUseCase;
import com.drivehub.identidad.domain.port.in.SesionEmitida;

import jakarta.validation.Valid;

/**
 * Inicio, renovación (CU02) y cierre de sesión (CU03). Los tres endpoints son públicos.
 *
 * * Web: el refresh token viaja SOLO en la cookie `refreshToken` (HttpOnly,
 * * SameSite=Lax, Path=/api/v1/auth); el código de la página nunca lo ve.
 * * Móvil: no hay cookies, el refresh token viaja en el cuerpo.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String COOKIE_REFRESH = "refreshToken";
    static final String RUTA_COOKIE = "/api/v1/auth";

    private final IniciarSesionUseCase iniciarSesion;
    private final RenovarSesionUseCase renovarSesion;
    private final CerrarSesionUseCase cerrarSesion;
    private final boolean cookieSegura;
    private final Duration vidaCookie;

    public AuthController(
            IniciarSesionUseCase iniciarSesion,
            RenovarSesionUseCase renovarSesion,
            CerrarSesionUseCase cerrarSesion,
            @Value("${app.cookies.secure:false}") boolean cookieSegura,
            @Value("${app.auth.refresh-ttl-dias:7}") long refreshTtlDias) {
        this.iniciarSesion = iniciarSesion;
        this.renovarSesion = renovarSesion;
        this.cerrarSesion = cerrarSesion;
        this.cookieSegura = cookieSegura;
        this.vidaCookie = Duration.ofDays(refreshTtlDias);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SesionResponse>> login(@Valid @RequestBody LoginRequest request) {
        SesionEmitida sesion = iniciarSesion.iniciar(
            new IniciarSesionComando(request.email(), request.password(), request.dispositivo()));
        return respuesta(sesion, request.dispositivo(), "Sesión iniciada correctamente");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SesionResponse>> refresh(
            @Valid @RequestBody RenovarRequest request,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshDeCookie) {
        String refreshToken = request.dispositivo() == Dispositivo.WEB ? refreshDeCookie : request.refreshToken();
        SesionEmitida sesion = renovarSesion.renovar(new RenovarSesionComando(refreshToken, request.dispositivo()));
        return respuesta(sesion, request.dispositivo(), "Sesión renovada correctamente");
    }

    /**
     * Idempotente: responde 200 aunque la sesión no exista o ya esté cerrada.
     * En la web siempre borra la cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RenovarRequest request,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshDeCookie) {
        String refreshToken = request.dispositivo() == Dispositivo.WEB ? refreshDeCookie : request.refreshToken();
        cerrarSesion.cerrar(new CerrarSesionComando(refreshToken, request.dispositivo()));

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (request.dispositivo() == Dispositivo.WEB) {
            respuesta.header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        }
        return respuesta.body(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    private ResponseEntity<ApiResponse<SesionResponse>> respuesta(SesionEmitida sesion, Dispositivo dispositivo, String mensaje) {
        boolean esWeb = dispositivo == Dispositivo.WEB;
        SesionResponse cuerpo = new SesionResponse(
            sesion.accessToken(), sesion.expiraEnSegundos(), aUsuarioResponse(sesion.usuario()),
            esWeb ? null : sesion.refreshToken());

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (esWeb) {
            respuesta.header(HttpHeaders.SET_COOKIE, cookie(sesion.refreshToken(), vidaCookie).toString());
        }
        return respuesta.body(ApiResponse.success(cuerpo, mensaje));
    }

    private SesionResponse.UsuarioSesionResponse aUsuarioResponse(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new SesionResponse.UsuarioSesionResponse(
            usuario.getCodigo(), usuario.getNombres(), usuario.getApellidos(), roles, usuario.getEscuelaCodigo());
    }

    ResponseCookie cookie(String valor, Duration vida) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
            .httpOnly(true)
            .secure(cookieSegura)
            .sameSite("Lax")
            .path(RUTA_COOKIE)
            .maxAge(vida)
            .build();
    }
}
