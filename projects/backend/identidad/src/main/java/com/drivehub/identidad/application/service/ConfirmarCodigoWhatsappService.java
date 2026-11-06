package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappUseCase;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

/**
 * CU47: comprobar el código que llegó al correo y vincular el número a la cuenta.
 *
 * * Un código incorrecto suma un intento y, al llegar al máximo, deja de servir. Por eso la transacción
 * * no se revierte con las excepciones de negocio: si se revirtiera, el intento fallido no quedaría guardado.
 * * Vincular reemplaza cualquier vínculo anterior del número y de la cuenta.
 * ! Nunca se loguea el número, el correo ni el código.
 */
@Service
public class ConfirmarCodigoWhatsappService implements ConfirmarCodigoWhatsappUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmarCodigoWhatsappService.class);
    private static final String MODULO = "whatsapp";
    private static final String MENSAJE = "El código no es válido o venció. Pide otro";

    private final UsuarioRepositoryPort usuarioRepository;
    private final DesafioWhatsappRepositoryPort desafioRepository;
    private final VinculoWhatsappRepositoryPort vinculoRepository;
    private final HasheadorWhatsappPort hasheador;
    private final CuentaDeWhatsapp cuentaDeWhatsapp;
    private final Clock reloj;
    private final int maxIntentos;

    public ConfirmarCodigoWhatsappService(
            UsuarioRepositoryPort usuarioRepository,
            DesafioWhatsappRepositoryPort desafioRepository,
            VinculoWhatsappRepositoryPort vinculoRepository,
            HasheadorWhatsappPort hasheador,
            CuentaDeWhatsapp cuentaDeWhatsapp,
            Clock reloj,
            @Value("${app.whatsapp.max-intentos:5}") int maxIntentos) {
        this.usuarioRepository = usuarioRepository;
        this.desafioRepository = desafioRepository;
        this.vinculoRepository = vinculoRepository;
        this.hasheador = hasheador;
        this.cuentaDeWhatsapp = cuentaDeWhatsapp;
        this.reloj = reloj;
        this.maxIntentos = maxIntentos;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirmar(ConfirmarCodigoWhatsappComando comando) {
        Instant ahora = Instant.now(reloj);
        String correo = comando.correo().trim().toLowerCase(Locale.ROOT);
        String telefonoHash = hasheador.hashearTelefono(comando.telefono());

        Usuario usuario = usuarioRepository.buscarPorEmail(correo).filter(cuentaDeWhatsapp::habilitada)
            .orElseThrow(this::codigoInvalido);
        DesafioWhatsapp desafio = desafioRepository.buscarVigente(usuario.getCodigo(), telefonoHash, ahora)
            .filter(d -> d.getIntentos() < maxIntentos)
            .orElseThrow(this::codigoInvalido);

        byte[] esperado = desafio.getCodigoHash().getBytes(StandardCharsets.UTF_8);
        byte[] recibido = hasheador.hashearCodigo(comando.codigo()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, recibido)) {
            desafioRepository.registrarIntentoFallido(desafio.getCodigo());
            LOG.warn("Código de WhatsApp incorrecto", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
            throw codigoInvalido();
        }

        desafioRepository.marcarUsado(desafio.getCodigo());
        vinculoRepository.reemplazar(usuario.getCodigo(), telefonoHash);
        LOG.info("WhatsApp vinculado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }

    private ValidationException codigoInvalido() {
        return new ValidationException(IdentidadErrorCodes.CODIGO_WHATSAPP_INVALIDO, MENSAJE);
    }
}
