package com.drivehub.academico.features.reservas.service;

import java.util.Set;

import com.drivehub.academico.features.reservas.client.ClienteDePagos;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * El punto de enchufe del orquestador (CU44): resuelve las reservas que quedaron a medias en ciertos estados.
 *
 * * Cada manejador es un bean de Spring. El {@link OrquestadorClases} los recibe todos, y el repositorio, el
 * * proceso programado y el conteo de intentos trabajan sobre la UNIÓN de sus {@link #estados()}: un manejador nuevo
 * * queda cubierto sin tocar el orquestador ni el job.
 * * Dos manejadores no pueden declarar el mismo estado: el servicio no arranca.
 * * Hoy hay uno: {@link ManejadorDeReserva} (`PENDIENTE_PAGO`, CU14).
 * ? Cómo se agrega CU15 (cancelar y reprogramar), la guía completa está en el README de las UT de CU44:
 * * 1. CU15 define sus estados transitorios en {@link EstadoReserva} (por ejemplo, «cancelación a medias»).
 * * 2. Crea un `@Component` que implementa esta interfaz y declara esos estados.
 * * 3. En {@link #resolver} termina la operación con el cliente que recibe, y para cancelar REUTILIZA
 * *    `OrquestadorClases.deshacerReserva` (no escribe otra copia de «devolver la hora y liberar la franja»).
 * * 4. Devuelve el {@link ResultadoOrquestacion} que corresponda; los intentos fallidos y la revisión manual
 * *    funcionan solos para sus estados.
 * ! El repositorio filtra por `estados()`: el manejador NUNCA debe tocar una reserva que no esté en sus estados.
 * ! Cada llamada a Pagos lleva una clave de idempotencia derivada del código de la reserva (repetir no cobra ni
 * ! devuelve dos veces) y el cliente recibido, no uno propio: así sirve con el Bearer del estudiante y con la
 * ! credencial de servicio.
 */
public interface ManejadorDeOperacion {

    /** Los estados transitorios que este manejador atiende. */
    Set<EstadoReserva> estados();

    /**
     * Resuelve UNA reserva que está en alguno de {@link #estados()}.
     *
     * @param cliente el cliente de Pagos que corresponde (el del estudiante en el flujo en vivo, el de servicio en
     *                el proceso programado)
     */
    ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente);
}
