package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SolicitudRegistroWhatsapp;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CompletarRegistroWhatsappUseCase;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.SolicitudRegistroWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

/**
 * CU48: con el enlace y la contraseña elegida en la web, crea la cuenta del estudiante y vincula su número.
 *
 * * La cuenta se crea con {@link AltaCuenta}, la misma regla del registro normal (CU01): no hay una copia.
 * * El número se vincula solo si está libre; si ya era de otra cuenta, la cuenta se crea igual y la persona
 * * recupera el chat con el inicio de sesión por código (CU47).
 * * Al terminar se borra la solicitud: sus datos personales no se conservan.
 * ! Nunca se loguea la contraseña, el enlace ni los datos personales.
 */
@Service
public class CompletarRegistroWhatsappService implements CompletarRegistroWhatsappUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CompletarRegistroWhatsappService.class);
    private static final String MODULO = "whatsapp";

    private final AltaCuenta altaCuenta;
    private final SolicitudRegistroWhatsappRepositoryPort solicitudRepository;
    private final VinculoWhatsappRepositoryPort vinculoRepository;
    private final EscuelaConsultaPort escuelaConsulta;
    private final GeneradorTokenOpacoPort generadorToken;
    private final HasheadorWhatsappPort hasheador;
    private final Clock reloj;
    private final UUID escuelaCodigo;

    public CompletarRegistroWhatsappService(
            AltaCuenta altaCuenta,
            SolicitudRegistroWhatsappRepositoryPort solicitudRepository,
            VinculoWhatsappRepositoryPort vinculoRepository,
            EscuelaConsultaPort escuelaConsulta,
            GeneradorTokenOpacoPort generadorToken,
            HasheadorWhatsappPort hasheador,
            Clock reloj,
            @Value("${app.whatsapp.escuela-codigo}") UUID escuelaCodigo) {
        this.altaCuenta = altaCuenta;
        this.solicitudRepository = solicitudRepository;
        this.vinculoRepository = vinculoRepository;
        this.escuelaConsulta = escuelaConsulta;
        this.generadorToken = generadorToken;
        this.hasheador = hasheador;
        this.reloj = reloj;
        this.escuelaCodigo = escuelaCodigo;
    }

    @Override
    @Transactional
    public boolean completar(String token, String password) {
        SolicitudRegistroWhatsapp solicitud = solicitudRepository.buscarPorTokenHash(generadorToken.hashear(token))
            .filter(s -> s.getExpiraEn().isAfter(Instant.now(reloj)))
            .orElseThrow(() -> new ValidationException(IdentidadErrorCodes.ENLACE_REGISTRO_INVALIDO,
                "El enlace no es válido o venció. Pídele otro al chat de WhatsApp"));

        Usuario cuenta = altaCuenta.crear(
            new AltaCuenta.Datos(solicitud.getNombres(), solicitud.getApellidos(), solicitud.getFechaNacimiento(),
                solicitud.getCorreo(), password, "+" + solicitud.getTelefono(), solicitud.getCi(),
                solicitud.getSexo(), solicitud.getLatitud(), solicitud.getLongitud()),
            this::escuelaDelChat, RolNombres.ESTUDIANTE, CategoriaNombres.SIN_CATEGORIA);

        String telefonoHash = hasheador.hashearTelefono(solicitud.getTelefono());
        boolean vinculado = vinculoRepository.buscarPorTelefonoHash(telefonoHash).isEmpty();
        if (vinculado) {
            vinculoRepository.reemplazar(cuenta.getCodigo(), telefonoHash);
        }
        solicitudRepository.eliminar(solicitud);

        LOG.info("Registro por WhatsApp completado", kv("module", MODULO),
            kv("usuarioCodigo", cuenta.getCodigo()), kv("vinculado", vinculado));
        return vinculado;
    }

    private UUID escuelaDelChat() {
        return escuelaConsulta.buscarPorCodigo(escuelaCodigo)
            .filter(escuela -> escuela.activa())
            .map(escuela -> escuela.codigo())
            .orElseThrow(() -> new NotFoundException(IdentidadErrorCodes.ESCUELA_NO_DISPONIBLE,
                "La escuela no existe o no está disponible"));
    }
}
