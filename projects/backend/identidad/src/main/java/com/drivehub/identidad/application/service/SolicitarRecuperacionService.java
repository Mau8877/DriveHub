package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarRecuperacionUseCase;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU04: pedir el enlace de recuperación de contraseña.
 *
 * * Solo recibe el enlace una cuenta que existe, no está inactiva y tiene el correo
 * * verificado (CU04b). Una cuenta BLOQUEADA por intentos sí: restablecer la desbloquea.
 * * Nunca falla por el estado de la cuenta ni por el envío: quien lo pide no debe poder
 * * averiguar qué correos existen. Un fallo del SMTP se registra como ERROR.
 * ! Nunca se loguea el correo recibido, el token ni el enlace.
 */
@Service
public class SolicitarRecuperacionService implements SolicitarRecuperacionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitarRecuperacionService.class);
    private static final String MODULO = "auth";

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenRecuperacionRepositoryPort tokenRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final EnviadorCorreoPort enviadorCorreo;
    private final Clock reloj;
    private final String urlWeb;
    private final Duration vigencia;

    public SolicitarRecuperacionService(
            UsuarioRepositoryPort usuarioRepository,
            TokenRecuperacionRepositoryPort tokenRepository,
            GeneradorTokenOpacoPort generadorToken,
            EnviadorCorreoPort enviadorCorreo,
            Clock reloj,
            @Value("${app.web.url}") String urlWeb,
            @Value("${app.auth.recuperacion-ttl-minutos:30}") long vigenciaMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.generadorToken = generadorToken;
        this.enviadorCorreo = enviadorCorreo;
        this.reloj = reloj;
        this.urlWeb = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    @Override
    @Transactional
    public void solicitar(String email) {
        String correo = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> cuenta = usuarioRepository.buscarPorEmail(correo)
            .filter(u -> u.getEstado() != EstadoUsuario.INACTIVO && u.isCorreoVerificado());
        if (cuenta.isEmpty()) {
            return;
        }
        Usuario usuario = cuenta.get();

        String token = generadorToken.generar();
        tokenRepository.invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.RECUPERACION);
        tokenRepository.guardar(TokenRecuperacion.builder()
            .usuarioCodigo(usuario.getCodigo())
            .tipo(TipoTokenCorreo.RECUPERACION)
            .tokenHash(generadorToken.hashear(token))
            .expiraEn(Instant.now(reloj).plus(vigencia))
            .build());

        try {
            enviadorCorreo.enviarRecuperacionContrasena(
                usuario.getEmail(), usuario.getNombres(), urlWeb + "/cuenta/restablecer?token=" + token);
            LOG.info("Correo de recuperacion enviado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
        } catch (RuntimeException e) {
            // * No se relanza: quien lo pidió no debe enterarse (no revelar cuentas)
            LOG.error("No se pudo enviar el correo de recuperacion", kv("module", MODULO),
                kv("usuarioCodigo", usuario.getCodigo()), e);
        }
    }
}
