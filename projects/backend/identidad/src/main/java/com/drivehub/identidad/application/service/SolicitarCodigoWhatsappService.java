package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.security.SecureRandom;
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

import com.drivehub.identidad.common.exception.LimiteExcedidoException;
import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappUseCase;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU47: enviar al correo de la cuenta el código para vincular el WhatsApp.
 *
 * * Termina igual exista o no la cuenta: no revela qué correos están registrados. Un pedido hacia un
 * * correo sin cuenta válida se guarda descartado, así el límite cuenta lo mismo en los dos casos.
 * * Si el envío del correo falla, la excepción se propaga y la transacción se revierte: no queda un
 * * código vigente que nadie recibió.
 * ! Nunca se loguea el número, el correo ni el código.
 */
@Service
public class SolicitarCodigoWhatsappService implements SolicitarCodigoWhatsappUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitarCodigoWhatsappService.class);
    private static final String MODULO = "whatsapp";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepositoryPort usuarioRepository;
    private final DesafioWhatsappRepositoryPort desafioRepository;
    private final HasheadorWhatsappPort hasheador;
    private final EnviadorCorreoPort enviadorCorreo;
    private final CuentaDeWhatsapp cuentaDeWhatsapp;
    private final Clock reloj;
    private final Duration vigencia;
    private final int maxPedidosPorHora;

    public SolicitarCodigoWhatsappService(
            UsuarioRepositoryPort usuarioRepository,
            DesafioWhatsappRepositoryPort desafioRepository,
            HasheadorWhatsappPort hasheador,
            EnviadorCorreoPort enviadorCorreo,
            CuentaDeWhatsapp cuentaDeWhatsapp,
            Clock reloj,
            @Value("${app.whatsapp.codigo-ttl-minutos:10}") long vigenciaMinutos,
            @Value("${app.whatsapp.max-pedidos-por-hora:5}") int maxPedidosPorHora) {
        this.usuarioRepository = usuarioRepository;
        this.desafioRepository = desafioRepository;
        this.hasheador = hasheador;
        this.enviadorCorreo = enviadorCorreo;
        this.cuentaDeWhatsapp = cuentaDeWhatsapp;
        this.reloj = reloj;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
        this.maxPedidosPorHora = maxPedidosPorHora;
    }

    @Override
    @Transactional
    public void solicitar(SolicitarCodigoWhatsappComando comando) {
        Instant ahora = Instant.now(reloj);
        String correo = comando.correo().trim().toLowerCase(Locale.ROOT);
        String telefonoHash = hasheador.hashearTelefono(comando.telefono());
        String correoHash = hasheador.hashearCorreo(correo);

        Instant desde = ahora.minus(Duration.ofHours(1));
        if (desafioRepository.contarPorTelefonoDesde(telefonoHash, desde) >= maxPedidosPorHora
                || desafioRepository.contarPorCorreoDesde(correoHash, desde) >= maxPedidosPorHora) {
            LOG.warn("Límite de pedidos de código de WhatsApp superado", kv("module", MODULO));
            throw new LimiteExcedidoException("Hiciste muchos pedidos. Espera un rato e inténtalo de nuevo");
        }

        Optional<Usuario> cuenta = usuarioRepository.buscarPorEmail(correo).filter(cuentaDeWhatsapp::habilitada);
        if (cuenta.isEmpty()) {
            // * Descartado desde el principio: nadie recibe nada, pero el pedido cuenta para el límite
            desafioRepository.guardar(DesafioWhatsapp.builder()
                .telefonoHash(telefonoHash)
                .correoHash(correoHash)
                .codigoHash(hasheador.hashearCodigo("descartado"))
                .expiraEn(ahora)
                .usado(true)
                .build());
            return;
        }

        Usuario usuario = cuenta.get();
        String codigo = "%06d".formatted(ALEATORIO.nextInt(1_000_000));
        desafioRepository.invalidarPendientes(usuario.getCodigo());
        desafioRepository.guardar(DesafioWhatsapp.builder()
            .usuarioCodigo(usuario.getCodigo())
            .telefonoHash(telefonoHash)
            .correoHash(correoHash)
            .codigoHash(hasheador.hashearCodigo(codigo))
            .expiraEn(ahora.plus(vigencia))
            .build());

        enviadorCorreo.enviarCodigoWhatsapp(usuario.getEmail(), usuario.getNombres(), codigo,
            (int) vigencia.toMinutes());
        LOG.info("Código de WhatsApp enviado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()));
    }
}
