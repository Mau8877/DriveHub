package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.escuela.application.dto.CrearEscuelaInput;
import com.drivehub.escuela.application.dto.EscuelaCreadaOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.domain.gateway.AdministradorEscuelaGateway;
import com.drivehub.escuela.domain.repository.EscuelaRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU05: el Super admin crea una escuela junto con su Administrador.
 *
 * * Son dos servicios con dos bases y no hay una transacción que abarque las dos. Se coordina
 * * como una saga SIMPLIFICADA, sin estado: la fila de la escuela se guarda en una transacción
 * * todavía ABIERTA, mientras tanto Identidad crea al Administrador, y solo si eso salió bien se
 * * confirma. El orden hace inofensivos casi todos los fallos:
 * * - Identidad rechaza (correo en uso, menor de edad...): la transacción se deshace y no hay
 * *   nada que compensar; el error de Identidad llega tal cual.
 * * - Identidad no responde, vence o falla la confirmación: no se sabe si la cuenta se creó, así
 * *   que se la revierte (idempotente) y se relanza el error original.
 * * - Si ni la compensación funciona, la cuenta sobrante no puede iniciar sesión (su escuela no
 * *   existe) y queda un ERROR en el registro para limpiarla.
 * ! Nunca se escribe la contraseña del Administrador en los registros.
 */
@Service
@RequiredArgsConstructor
public class CrearEscuelaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearEscuelaUseCase.class);
    private static final String MODULO = "escuelas";
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int SLUG_MIN = 3;
    private static final int SLUG_MAX = 60;
    private static final int NOMBRE_MIN = 3;
    private static final int NOMBRE_MAX = 120;

    private final EscuelaRepository escuelaRepository;
    private final AdministradorEscuelaGateway administradorGateway;
    private final TransactionTemplate transaccion;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EscuelaCreadaOutput ejecutar(CrearEscuelaInput input) {
        String nombre = input.nombre() == null ? "" : input.nombre().trim();
        String slug = input.slug() == null ? "" : input.slug().trim().toLowerCase(Locale.ROOT);
        validar(nombre, slug);

        if (escuelaRepository.existePorSlug(slug)) {
            throw slugEnUso();
        }

        UUID codigo = UuidV7.generar();
        AtomicReference<Escuela> escuela = new AtomicReference<>();
        AtomicReference<AdministradorCreado> administrador = new AtomicReference<>();
        try {
            transaccion.executeWithoutResult(estado -> {
                escuela.set(escuelaRepository.guardar(
                    Escuela.builder().codigo(codigo).slug(slug).nombre(nombre).build()));
                administrador.set(administradorGateway.crearAdministrador(codigo, input.administrador()));
            });
        } catch (BusinessException e) {
            // * Rechazo de negocio (slug en uso, o Identidad rechazó): la transacción ya se deshizo
            // * y Identidad no creó nada, no hay nada que compensar.
            throw e;
        } catch (RuntimeException e) {
            compensar(codigo);
            throw e;
        }

        LOG.info("Escuela creada", kv("module", MODULO), kv("escuelaCodigo", codigo), kv("slug", slug));
        Escuela creada = escuela.get();
        return new EscuelaCreadaOutput(creada.getCodigo(), creada.getSlug(), creada.getNombre(), creada.estado(),
            creada.getCreadoEn(), administrador.get());
    }

    private void validar(String nombre, String slug) {
        if (nombre.length() < NOMBRE_MIN || nombre.length() > NOMBRE_MAX) {
            throw rechazoDeValidacion("El nombre debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX + " caracteres");
        }
        if (slug.length() < SLUG_MIN || slug.length() > SLUG_MAX || !SLUG.matcher(slug).matches()) {
            throw rechazoDeValidacion("El slug debe tener entre " + SLUG_MIN + " y " + SLUG_MAX
                + " caracteres: letras minúsculas, números y guiones, sin guion al inicio ni al final");
        }
    }

    private ValidationException rechazoDeValidacion(String mensaje) {
        LOG.warn("Escuela rechazada: datos inválidos",
            kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.VALIDACION));
        return new ValidationException(EscuelaErrorCodes.VALIDACION, mensaje);
    }

    private ConflictException slugEnUso() {
        LOG.warn("Escuela rechazada: slug en uso",
            kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.SLUG_EN_USO));
        return new ConflictException(EscuelaErrorCodes.SLUG_EN_USO, "Ya existe una escuela con ese slug");
    }

    /**
     * Deshace en Identidad lo que pudo haberse creado. Nunca lanza: si falla, el error original es
     * el que importa y la cuenta sobrante queda registrada para limpiarla.
     */
    private void compensar(UUID escuelaCodigo) {
        try {
            administradorGateway.revertirAdministrador(escuelaCodigo);
            LOG.warn("Creación de escuela fallida: se revirtió al administrador",
                kv("module", MODULO), kv("escuelaCodigo", escuelaCodigo));
        } catch (RuntimeException e) {
            LOG.error("Compensación fallida: puede quedar un administrador sin escuela",
                kv("module", MODULO), kv("escuelaCodigo", escuelaCodigo), e);
        }
    }
}
