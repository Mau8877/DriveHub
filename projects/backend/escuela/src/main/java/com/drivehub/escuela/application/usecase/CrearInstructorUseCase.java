package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.escuela.application.dto.CrearInstructorInput;
import com.drivehub.escuela.application.dto.InstructorDetalleOutput;
import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.entity.NroLicencia;
import com.drivehub.escuela.domain.gateway.DatosCuentaInstructor;
import com.drivehub.escuela.domain.gateway.InstructorCuentaGateway;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU09: el Administrador de escuela da de alta a un instructor: su cuenta y perfil en Identidad y su
 * ficha profesional en Escuela.
 *
 * * Son dos servicios con dos bases y no hay una transacción que abarque las dos. Se coordina como una
 * * saga SIMPLE, sin estado, y su orden hace inofensivos casi todos los fallos:
 * * 1. Se valida todo lo propio (formato, fechas, categorías, licencia única) ANTES de tocar Identidad.
 * * 2. Escuela genera el código de la persona (`usuarioCodigo`) y se lo manda a Identidad al crear la
 * *    cuenta: así puede revertirla aunque la respuesta se pierda por un timeout.
 * * 3. Identidad rechaza (correo en uso, menor de edad...): no creó nada, el error llega tal cual.
 * * 4. Identidad no responde, vence, o falla guardar la ficha (incluida una carrera por la licencia):
 * *    el resultado allá es incierto o la ficha no existe, así que se REVIERTE la cuenta (idempotente)
 * *    y se relanza el error original.
 * * 5. Si ni la compensación funciona, queda un ERROR en el registro: la cuenta sobrante no aparece en
 * *    ningún listado (manda la ficha), pero su correo y su CI quedan ocupados hasta limpiarla.
 * ! Nunca se escribe la contraseña, el CI, el correo, el teléfono, el número de licencia ni las
 * ! coordenadas en los registros.
 */
@Service
@RequiredArgsConstructor
public class CrearInstructorUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CrearInstructorUseCase.class);
    private static final String MODULO = "instructores";

    private final InstructorRepository instructorRepository;
    private final CategoriaLicenciaRepository categoriaRepository;
    private final InstructorCuentaGateway cuentaGateway;
    private final InstructorMapeador mapeador;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public InstructorDetalleOutput ejecutar(CrearInstructorInput input) {
        LocalDate hoy = LocalDate.now(reloj);

        // 1. Lo propio, antes de tocar Identidad
        String nroLicencia = NroLicencia.normalizarYValidar(input.nroLicencia());
        Instructor.validarVencimiento(input.licenciaVence(), hoy);
        if (input.fechaContratacion() != null) {
            Instructor.validarContratacion(input.fechaContratacion(), hoy);
        }
        List<String> siglas = Instructor.validarSiglas(input.categorias());
        if (instructorRepository.existePorNroLicencia(nroLicencia)) {
            LOG.warn("Alta rechazada: número de licencia en uso",
                kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.NRO_LICENCIA_EN_USO));
            throw new ConflictException(EscuelaErrorCodes.NRO_LICENCIA_EN_USO,
                "El número de licencia ya pertenece a otro instructor");
        }
        Set<CategoriaLicencia> categorias = categoriasValidas(siglas);

        UUID usuarioCodigo = UuidV7.generar();
        Instructor ficha = Instructor.nuevo(UuidV7.generar(), usuarioCodigo, input.escuelaCodigo(), nroLicencia,
            input.licenciaVence(), input.fechaContratacion(), categorias, hoy);

        // 2 y 3. La cuenta en Identidad; si la rechaza por una regla de negocio no hay nada que compensar
        PersonaInstructor persona;
        try {
            persona = cuentaGateway.crearCuenta(new DatosCuentaInstructor(usuarioCodigo, input.nombres(),
                input.apellidos(), input.fechaNacimiento(), input.sexo(), input.ci(), input.telefono(),
                input.email(), input.password(), input.latitud(), input.longitud()));
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            compensar(usuarioCodigo);
            throw e;
        }

        // 4. La ficha; si falla, se deshace la cuenta
        Instructor guardada;
        try {
            guardada = transaccion.execute(estado -> instructorRepository.guardar(ficha));
        } catch (RuntimeException e) {
            compensar(usuarioCodigo);
            throw e;
        }

        LOG.info("Instructor creado", kv("module", MODULO), kv("instructorCodigo", guardada.getCodigo()),
            kv("escuelaCodigo", guardada.getEscuelaCodigo()));
        return mapeador.aDetalle(guardada, persona);
    }

    /** Todas las siglas existen en el catálogo y ninguna es "sin categoría"; si no, `ERR_ESC_10`. */
    private Set<CategoriaLicencia> categoriasValidas(List<String> siglas) {
        List<CategoriaLicencia> encontradas = categoriaRepository.buscarPorSiglas(siglas);
        boolean hayInvalida = siglas.contains(Instructor.SIN_CATEGORIA) || encontradas.size() != siglas.size();
        if (hayInvalida) {
            LOG.warn("Alta rechazada: categoría inválida",
                kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.CATEGORIA_INVALIDA));
            throw new ValidationException(EscuelaErrorCodes.CATEGORIA_INVALIDA,
                "Alguna categoría no existe o no se puede enseñar");
        }
        return new LinkedHashSet<>(encontradas);
    }

    /**
     * Deshace en Identidad lo que pudo haberse creado. Nunca lanza: si falla, el error original es el
     * que importa y la cuenta sobrante queda registrada para limpiarla.
     */
    private void compensar(UUID usuarioCodigo) {
        try {
            cuentaGateway.revertirCuenta(usuarioCodigo);
            LOG.warn("Alta de instructor fallida: se revirtió la cuenta",
                kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo));
        } catch (RuntimeException e) {
            LOG.error("Compensación fallida: puede quedar una cuenta sin ficha",
                kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo), e);
        }
    }
}
