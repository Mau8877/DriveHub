package com.drivehub.pagos.features.billetera.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.common.util.UuidV7;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.billetera.model.BilleteraHoras;
import com.drivehub.pagos.features.billetera.model.MovimientoHoras;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;

import lombok.RequiredArgsConstructor;

/**
 * La billetera de horas de cada estudiante (CU27): acreditar las horas de una compra confirmada, consultar el
 * saldo y gastar o devolver horas (lo que usa Académico, ver CONTRATO_BILLETERA_HORAS.md).
 *
 * * Toda operación sobre una billetera la lee con BLOQUEO de escritura: dos operaciones sobre la misma billetera
 * * se hacen una después de la otra, así el saldo nunca se pisa ni queda negativo (la base lo exige además con
 * * un CHECK, y `version` es la última defensa).
 * * Las horas se gastan de las compras MÁS ANTIGUAS primero (FIFO) y cada movimiento apunta a la compra de la
 * * que salen: es lo que permite que el reembolso sea "todo o nada" por compra (CU31) y que CU29 sepa cuánto
 * * gastó cada alumno con cada instructor.
 * * Acreditar, consumir y devolver son idempotentes: repetir la operación (misma clave) no la repite.
 * * `acreditar` lo llama el sistema, sin sesión de nadie: no lleva `@PreAuthorize`.
 * ! No se loguean datos del estudiante; solo códigos.
 */
@Service
@RequiredArgsConstructor
public class BilleteraService {

    static final int HORAS_MAXIMAS = 500;
    static final int CLAVE_MAXIMA = 64;

    private static final Logger LOG = LoggerFactory.getLogger(BilleteraService.class);
    private static final String MODULO = "billetera";
    private static final String ROLES_QUE_GASTAN = "hasAnyRole('INSTRUCTOR','ADMIN_ESCUELA') or "
        + "(hasRole('ESTUDIANTE') and #solicitanteCodigo == #estudianteCodigo)";

    private final BilleteraRepository billeteraRepository;
    private final MovimientoHorasRepository movimientoRepository;
    private final PagoRepository pagoRepository;

    // ------------------------------------------------------------------ acreditar y saldo

    /**
     * Suma a la billetera del estudiante las horas del pago (la crea en la primera compra), deja en el pago las
     * horas sin gastar y guarda el movimiento. Se une a la transacción de quien llama: si algo falla después, todo
     * se deshace junto.
     *
     * @return {@code false} si ese pago ya estaba acreditado (un aviso repetido): no cambia nada
     */
    @Transactional
    public boolean acreditar(Pago pago) {
        String clave = claveDeAcreditacion(pago);
        // * Primero el bloqueo y recién después la comprobación: si dos avisos del mismo pago llegan a la vez, el
        // * segundo espera acá a que el primero termine y entonces ve que ya se acreditó
        BilleteraHoras billetera = billeteraParaActualizar(pago.getEscuelaCodigo(), pago.getEstudianteCodigo());
        if (movimientoRepository.existsByIdempotencyKeyAndPagoId(clave, pago.getId())) {
            return false;
        }
        int horas = pago.getHoras();
        billetera.setHorasDisponibles(billetera.getHorasDisponibles() + horas);
        billeteraRepository.save(billetera);
        movimientoRepository.save(new MovimientoHoras(billetera, pago, TipoMovimiento.ACREDITACION, horas, null, null,
            clave));
        pago.setHorasRestantes(pago.getHoras());

        LOG.info("Horas acreditadas", kv("module", MODULO), kv("pagoCodigo", pago.getCodigo()),
            kv("escuelaCodigo", pago.getEscuelaCodigo()), kv("horas", horas));
        return true;
    }

    /**
     * Las horas disponibles del estudiante, o 0 si todavía no tiene billetera.
     */
    @Transactional(readOnly = true)
    public int saldo(UUID escuelaCodigo, UUID estudianteCodigo) {
        return billeteraRepository.findByEscuelaCodigoAndEstudianteCodigo(escuelaCodigo, estudianteCodigo)
            .map(BilleteraHoras::getHorasDisponibles).orElse(0);
    }

    /** El saldo del propio estudiante que consulta. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public int miSaldo(UUID escuelaCodigo, UUID estudianteCodigo) {
        return saldo(escuelaCodigo, estudianteCodigo);
    }

    /** El saldo de un estudiante de la escuela, para quien lo atiende. */
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN_ESCUELA')")
    @Transactional(readOnly = true)
    public int saldoDe(UUID escuelaCodigo, UUID estudianteCodigo) {
        return saldo(escuelaCodigo, estudianteCodigo);
    }

    // ------------------------------------------------------------------ consumir y devolver

    /**
     * Gasta horas de la billetera del estudiante (Académico, al reservar o dar una clase).
     *
     * * El instructor, el estudiante o el Administrador de la escuela pueden gastar; un estudiante solo sobre su
     * * propia billetera (`solicitanteCodigo` es quien hace la petición).
     * * Con la misma clave de idempotencia devuelve lo que ya descontó, sin descontar de nuevo.
     * * Sin horas suficientes responde `ERR_PAG_09` y no cambia nada.
     *
     * @param horas entero de 1 a 500
     * @param instructorCodigo con quién es la clase: de ahí sale cuánto gastó cada alumno con cada instructor
     * @param claseCodigo la clase que gasta las horas (para poder devolverlas)
     */
    @PreAuthorize(ROLES_QUE_GASTAN)
    @Transactional
    public ConsumoResultado consumir(UUID escuelaCodigo, UUID solicitanteCodigo, UUID estudianteCodigo, int horas,
            UUID instructorCodigo, UUID claseCodigo, String idempotencyKey) {
        validarClave(idempotencyKey);
        if (horas < 1 || horas > HORAS_MAXIMAS) {
            throw new ValidationException(PagosErrorCodes.VALIDACION,
                "Las horas deben ser un número entero de 1 a " + HORAS_MAXIMAS);
        }
        if (instructorCodigo == null || claseCodigo == null) {
            throw new ValidationException(PagosErrorCodes.VALIDACION, "Indica el instructor y la clase");
        }
        BilleteraHoras billetera = billeteraRepository.buscarParaActualizar(escuelaCodigo, estudianteCodigo)
            .orElseThrow(BilleteraService::horasInsuficientes);

        List<MovimientoHoras> previos = movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(
            billetera.getId(), idempotencyKey, TipoMovimiento.CONSUMO);
        if (!previos.isEmpty()) {
            return new ConsumoResultado(sumar(previos), billetera.getHorasDisponibles());
        }
        if (billetera.getHorasDisponibles() < horas) {
            throw horasInsuficientes();
        }

        int falta = horas;
        for (Pago compra : pagoRepository.confirmadosConHorasRestantes(escuelaCodigo, estudianteCodigo)) {
            int quedan = compra.getHorasRestantes();
            int tomar = Math.min(quedan, falta);
            if (tomar <= 0) {
                continue;
            }
            compra.setHorasRestantes((short) (quedan - tomar));
            movimientoRepository.save(new MovimientoHoras(billetera, compra, TipoMovimiento.CONSUMO, tomar,
                instructorCodigo, claseCodigo, idempotencyKey));
            falta -= tomar;
            if (falta == 0) {
                break;
            }
        }
        if (falta > 0) {
            // * El saldo y las compras no cuadran: no se descuenta nada (la transacción se deshace)
            LOG.error("El saldo de la billetera no coincide con las horas de sus compras", kv("module", MODULO),
                kv("escuelaCodigo", escuelaCodigo));
            throw horasInsuficientes();
        }
        billetera.setHorasDisponibles(billetera.getHorasDisponibles() - horas);
        billeteraRepository.save(billetera);

        LOG.info("Horas consumidas", kv("module", MODULO), kv("claseCodigo", claseCodigo), kv("horas", horas));
        return new ConsumoResultado(horas, billetera.getHorasDisponibles());
    }

    /**
     * Devuelve a la billetera las horas que gastó una clase (se canceló). Vuelven a las MISMAS compras de las
     * que salieron, con el mismo instructor y la misma clase en el movimiento.
     *
     * * Con la misma clave devuelve lo que ya devolvió, sin devolver de nuevo.
     * * Si esa clase no tiene nada para devolver (nunca gastó o ya se devolvió todo), `ERR_PAG_16`.
     */
    @PreAuthorize(ROLES_QUE_GASTAN)
    @Transactional
    public DevolucionResultado devolver(UUID escuelaCodigo, UUID solicitanteCodigo, UUID estudianteCodigo,
            UUID claseCodigo, String idempotencyKey) {
        validarClave(idempotencyKey);
        if (claseCodigo == null) {
            throw new ValidationException(PagosErrorCodes.VALIDACION, "Indica la clase");
        }
        BilleteraHoras billetera = billeteraRepository.buscarParaActualizar(escuelaCodigo, estudianteCodigo)
            .orElseThrow(BilleteraService::sinConsumoParaDevolver);

        List<MovimientoHoras> previos = movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(
            billetera.getId(), idempotencyKey, TipoMovimiento.DEVOLUCION);
        if (!previos.isEmpty()) {
            return new DevolucionResultado(sumar(previos), billetera.getHorasDisponibles());
        }

        List<MovimientoHoras> consumos = movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(
            billetera.getId(), claseCodigo, TipoMovimiento.CONSUMO);
        List<MovimientoHoras> devueltos = movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(
            billetera.getId(), claseCodigo, TipoMovimiento.DEVOLUCION);

        // * Lo que esa clase gastó de cada compra, menos lo que ya se le devolvió
        Map<Long, Integer> porDevolver = new LinkedHashMap<>();
        Map<Long, MovimientoHoras> origen = new LinkedHashMap<>();
        for (MovimientoHoras consumo : consumos) {
            Long compraId = consumo.getPago().getId();
            porDevolver.merge(compraId, (int) consumo.getHoras(), Integer::sum);
            origen.putIfAbsent(compraId, consumo);
        }
        for (MovimientoHoras devuelto : devueltos) {
            porDevolver.merge(devuelto.getPago().getId(), -(int) devuelto.getHoras(), Integer::sum);
        }
        List<Long> compras = new ArrayList<>(porDevolver.keySet());
        compras.removeIf(compraId -> porDevolver.get(compraId) <= 0);
        if (compras.isEmpty()) {
            throw sinConsumoParaDevolver();
        }

        int total = 0;
        for (Long compraId : compras) {
            int horas = porDevolver.get(compraId);
            Pago compra = pagoRepository.buscarParaActualizar(compraId).orElseThrow();
            compra.setHorasRestantes((short) (compra.getHorasRestantes() + horas));
            MovimientoHoras consumo = origen.get(compraId);
            movimientoRepository.save(new MovimientoHoras(billetera, compra, TipoMovimiento.DEVOLUCION, horas,
                consumo.getInstructorCodigo(), consumo.getClaseCodigo(), idempotencyKey));
            total += horas;
        }
        billetera.setHorasDisponibles(billetera.getHorasDisponibles() + total);
        billeteraRepository.save(billetera);

        LOG.info("Horas devueltas", kv("module", MODULO), kv("claseCodigo", claseCodigo), kv("horas", total));
        return new DevolucionResultado(total, billetera.getHorasDisponibles());
    }

    // ------------------------------------------------------------------ utilidades

    static String claveDeAcreditacion(Pago pago) {
        return "acreditacion:" + pago.getCodigo();
    }

    private static int sumar(List<MovimientoHoras> movimientos) {
        return movimientos.stream().mapToInt(MovimientoHoras::getHoras).sum();
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > CLAVE_MAXIMA) {
            throw new ValidationException(PagosErrorCodes.VALIDACION,
                "La clave de la operación es obligatoria (de 1 a " + CLAVE_MAXIMA + " caracteres)");
        }
    }

    private static ConflictException horasInsuficientes() {
        return new ConflictException(PagosErrorCodes.HORAS_INSUFICIENTES, "No hay horas suficientes en la billetera");
    }

    private static NotFoundException sinConsumoParaDevolver() {
        return new NotFoundException(PagosErrorCodes.CONSUMO_NO_ENCONTRADO,
            "No hay horas gastadas en esa clase para devolver");
    }

    /**
     * La billetera con bloqueo de escritura; si no existe, la crea vacía (sin romper la transacción si otra
     * operación la creó a la vez) y la lee.
     */
    private BilleteraHoras billeteraParaActualizar(UUID escuelaCodigo, UUID estudianteCodigo) {
        billeteraRepository.crearSiNoExiste(UuidV7.generar(), escuelaCodigo, estudianteCodigo);
        Optional<BilleteraHoras> billetera = billeteraRepository.buscarParaActualizar(escuelaCodigo, estudianteCodigo);
        return billetera.orElseThrow(() -> new IllegalStateException("La billetera debería existir"));
    }
}
