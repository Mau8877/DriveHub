package com.drivehub.identidad.infrastructure.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.drivehub.identidad.domain.port.out.SolicitudRegistroWhatsappRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Borra las solicitudes de registro por WhatsApp (CU48) cuyo enlace venció sin que la persona fijara su
 * contraseña: son datos personales y no se conservan.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class LimpiezaWhatsappConfig {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaWhatsappConfig.class);

    private final SolicitudRegistroWhatsappRepositoryPort solicitudes;
    private final Clock reloj;

    @Scheduled(fixedDelayString = "${app.whatsapp.limpieza-ms:600000}", initialDelayString = "${app.whatsapp.limpieza-ms:600000}")
    public void borrarSolicitudesVencidas() {
        int borradas = solicitudes.eliminarVencidas(Instant.now(reloj));
        if (borradas > 0) {
            LOG.info("Solicitudes de registro por WhatsApp vencidas borradas", kv("module", "whatsapp"),
                kv("cantidad", borradas));
        }
    }
}
