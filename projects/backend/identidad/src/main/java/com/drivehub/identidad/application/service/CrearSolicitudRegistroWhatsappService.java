package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.Ci;
import com.drivehub.identidad.domain.model.SolicitudRegistroWhatsapp;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappComando;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.EnlaceRegistro;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.SolicitudRegistroWhatsappRepositoryPort;

/**
 * CU48: guarda lo que la persona dictó por el chat y entrega el enlace para elegir la contraseña.
 *
 * * Vuelve a comprobar todo lo que depende de los datos guardados (edad, correo y CI sin usar): entre un dato y
 * * el siguiente otra persona pudo registrar el mismo correo o CI.
 * * Una solicitud nueva del mismo número reemplaza a las anteriores. Todavía no hay cuenta.
 * ! Nunca se loguea el enlace, el token ni los datos personales.
 */
@Service
public class CrearSolicitudRegistroWhatsappService implements CrearSolicitudRegistroWhatsappUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearSolicitudRegistroWhatsappService.class);
    private static final String MODULO = "whatsapp";

    private final AltaCuenta altaCuenta;
    private final SolicitudRegistroWhatsappRepositoryPort solicitudRepository;
    private final GeneradorTokenOpacoPort generadorToken;
    private final Clock reloj;
    private final String urlWeb;
    private final Duration vigencia;

    public CrearSolicitudRegistroWhatsappService(
            AltaCuenta altaCuenta,
            SolicitudRegistroWhatsappRepositoryPort solicitudRepository,
            GeneradorTokenOpacoPort generadorToken,
            Clock reloj,
            @Value("${app.web.url}") String urlWeb,
            @Value("${app.whatsapp.registro-ttl-minutos:30}") long vigenciaMinutos) {
        this.altaCuenta = altaCuenta;
        this.solicitudRepository = solicitudRepository;
        this.generadorToken = generadorToken;
        this.reloj = reloj;
        this.urlWeb = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    @Override
    @Transactional
    public EnlaceRegistro crear(CrearSolicitudRegistroWhatsappComando comando) {
        String correo = comando.correo().trim().toLowerCase(Locale.ROOT);
        String ci = Ci.normalizar(comando.ci());
        altaCuenta.verificarEdadMinima(comando.fechaNacimiento());
        altaCuenta.verificarCorreoLibre(correo);
        altaCuenta.verificarCiLibre(ci);

        String telefono = comando.telefono().replaceAll("\\D", "");
        String token = generadorToken.generar();
        Instant expiraEn = Instant.now(reloj).plus(vigencia);
        solicitudRepository.eliminarDelTelefono(telefono);
        SolicitudRegistroWhatsapp guardada = solicitudRepository.guardar(SolicitudRegistroWhatsapp.builder()
            .telefono(telefono)
            .nombres(comando.nombres().trim())
            .apellidos(comando.apellidos().trim())
            .fechaNacimiento(comando.fechaNacimiento())
            .sexo(comando.sexo())
            .ci(ci)
            .correo(correo)
            .latitud(comando.latitud())
            .longitud(comando.longitud())
            .tokenHash(generadorToken.hashear(token))
            .expiraEn(expiraEn)
            .build());

        LOG.info("Solicitud de registro por WhatsApp creada", kv("module", MODULO),
            kv("solicitudCodigo", guardada.getCodigo()));
        return new EnlaceRegistro(urlWeb + "/account/set-password?token=" + token, expiraEn);
    }
}
