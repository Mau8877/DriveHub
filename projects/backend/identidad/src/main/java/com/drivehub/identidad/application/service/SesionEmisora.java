package com.drivehub.identidad.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.AccesoPorPlataforma;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;

/**
 * Lo que comparten iniciar y renovar sesión (CU02): comprobar que la escuela y
 * la plataforma permiten entrar, y emitir el token de acceso con una sesión
 * renovable nueva y la URL de la foto de la cuenta (CU06).
 */
@Component
public class SesionEmisora {

    private final EscuelaConsultaPort escuelaConsulta;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final EmisorTokenAccesoPort emisorTokenAcceso;
    private final GeneradorTokenOpacoPort generadorTokenOpaco;
    private final PerfilArmador perfilArmador;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public SesionEmisora(
            EscuelaConsultaPort escuelaConsulta,
            RefreshTokenRepositoryPort refreshTokenRepository,
            EmisorTokenAccesoPort emisorTokenAcceso,
            GeneradorTokenOpacoPort generadorTokenOpaco,
            PerfilArmador perfilArmador,
            @Value("${app.auth.access-ttl-minutos:15}") long accessTtlMinutos,
            @Value("${app.auth.refresh-ttl-dias:7}") long refreshTtlDias) {
        this.escuelaConsulta = escuelaConsulta;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emisorTokenAcceso = emisorTokenAcceso;
        this.generadorTokenOpaco = generadorTokenOpaco;
        this.perfilArmador = perfilArmador;
        this.accessTtl = Duration.ofMinutes(accessTtlMinutos);
        this.refreshTtl = Duration.ofDays(refreshTtlDias);
    }

    /** La escuela de la cuenta debe existir y estar activa; el Super admin no tiene escuela. */
    public void verificarEscuelaActiva(Usuario usuario) {
        if (usuario.getEscuelaCodigo() == null) {
            boolean esSuperAdmin = usuario.getRoles().stream().anyMatch(r -> RolNombres.SUPER_ADMIN.equals(r.getNombre()));
            if (esSuperAdmin) {
                return;
            }
            throw escuelaSuspendida();
        }
        boolean activa = escuelaConsulta.buscarPorCodigo(usuario.getEscuelaCodigo()).map(EscuelaRef::activa).orElse(false);
        if (!activa) {
            throw escuelaSuspendida();
        }
    }

    /** El rol de la cuenta debe poder usar la plataforma desde la que entra. */
    public void verificarPlataforma(Usuario usuario, Dispositivo dispositivo) {
        if (!AccesoPorPlataforma.permite(dispositivo, usuario.getRoles())) {
            throw new ForbiddenException(IdentidadErrorCodes.PLATAFORMA_NO_PERMITIDA,
                "Tu cuenta no puede usar esta plataforma. Ingresa desde " + AccesoPorPlataforma.plataformaCorrecta(dispositivo));
        }
    }

    /** Guarda una sesión renovable nueva y emite el token de acceso. */
    public SesionEmitida emitir(Usuario usuario, Dispositivo dispositivo, Instant ahora) {
        String refreshEnClaro = generadorTokenOpaco.generar();
        refreshTokenRepository.guardar(RefreshToken.builder()
            .usuarioCodigo(usuario.getCodigo())
            .tokenHash(generadorTokenOpaco.hashear(refreshEnClaro))
            .dispositivo(dispositivo)
            .expiraEn(ahora.plus(refreshTtl))
            .revocado(false)
            .build());
        String accessToken = emisorTokenAcceso.emitir(usuario, ahora);
        return new SesionEmitida(accessToken, accessTtl.toSeconds(), refreshEnClaro, usuario,
            perfilArmador.urlDeFoto(usuario));
    }

    private ForbiddenException escuelaSuspendida() {
        return new ForbiddenException(IdentidadErrorCodes.ESCUELA_SUSPENDIDA, "Tu escuela está inactiva. Contacta al administrador de la plataforma");
    }
}
