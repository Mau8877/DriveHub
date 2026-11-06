package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.model.VinculoWhatsapp;
import com.drivehub.identidad.domain.port.in.EmitirSesionWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.SesionWhatsapp;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

/**
 * CU47: la sesión corta de quien escribe desde un número vinculado.
 *
 * * Es un access token normal (mismos claims y roles que el de la app) pero de pocos minutos y sin sesión
 * * renovable: cada servicio lo valida igual, así el chat nunca puede más que la persona.
 * * Solo la pide el flujo de n8n, con la credencial de servicio.
 * ! El token nunca se loguea.
 */
@Service
public class EmitirSesionWhatsappService implements EmitirSesionWhatsappUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EmitirSesionWhatsappService.class);
    private static final String MODULO = "whatsapp";

    private final VinculoWhatsappRepositoryPort vinculoRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final HasheadorWhatsappPort hasheador;
    private final CuentaDeWhatsapp cuentaDeWhatsapp;
    private final SesionEmisora sesionEmisora;
    private final EmisorTokenAccesoPort emisorTokenAcceso;
    private final Clock reloj;
    private final Duration vigencia;

    public EmitirSesionWhatsappService(
            VinculoWhatsappRepositoryPort vinculoRepository,
            UsuarioRepositoryPort usuarioRepository,
            HasheadorWhatsappPort hasheador,
            CuentaDeWhatsapp cuentaDeWhatsapp,
            SesionEmisora sesionEmisora,
            EmisorTokenAccesoPort emisorTokenAcceso,
            Clock reloj,
            @Value("${app.whatsapp.sesion-segundos:300}") long vigenciaSegundos) {
        this.vinculoRepository = vinculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.hasheador = hasheador;
        this.cuentaDeWhatsapp = cuentaDeWhatsapp;
        this.sesionEmisora = sesionEmisora;
        this.emisorTokenAcceso = emisorTokenAcceso;
        this.reloj = reloj;
        this.vigencia = Duration.ofSeconds(vigenciaSegundos);
    }

    @Override
    public SesionWhatsapp emitir(String telefono) {
        VinculoWhatsapp vinculo = vinculoRepository.buscarPorTelefonoHash(hasheador.hashearTelefono(telefono))
            .orElseThrow(() -> new NotFoundException(IdentidadErrorCodes.WHATSAPP_NO_VINCULADO,
                "Este número de WhatsApp no está vinculado a ninguna cuenta"));
        Usuario usuario = usuarioRepository.buscarPorCodigo(vinculo.getUsuarioCodigo())
            .filter(cuentaDeWhatsapp::habilitada)
            .orElseThrow(() -> new ForbiddenException(IdentidadErrorCodes.WHATSAPP_CUENTA_NO_HABILITADA,
                "Esta cuenta no puede usar el chat de WhatsApp"));
        // * Igual que al iniciar sesión: si la escuela está inactiva, la cuenta no entra
        sesionEmisora.verificarEscuelaActiva(usuario);

        Instant ahora = Instant.now(reloj);
        String accessToken = emisorTokenAcceso.emitir(usuario, ahora, vigencia);
        LOG.info("Sesión de WhatsApp emitida", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
        return new SesionWhatsapp(accessToken, vigencia.toSeconds());
    }
}
