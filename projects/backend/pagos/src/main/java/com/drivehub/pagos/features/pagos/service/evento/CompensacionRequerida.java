package com.drivehub.pagos.features.pagos.service.evento;

import java.util.UUID;

/**
 * Se cobró algo que no se puede dejar como está: hay que devolver el cobro completo (CU27). Lo escucha CU31, que
 * lo devuelve. Se publica dentro de la transacción y se escucha después del commit; es interno de Pagos.
 *
 * * Puede publicarse más de una vez para el mismo pago (la pasarela reintenta sus avisos): quien lo escucha tiene
 * * que ser idempotente.
 * * Lo escucha `CompensacionListener` (CU31), que crea la reversión del 100 % y pide la devolución.
 *
 * @param cobroReferencia el id del cobro en la pasarela: lo que hace falta para devolverlo
 */
public record CompensacionRequerida(UUID pagoCodigo, MotivoCompensacion motivo, String cobroReferencia) {
}
