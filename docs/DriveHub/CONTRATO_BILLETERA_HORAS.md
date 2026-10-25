# Contrato de la billetera de horas (Pagos ↔ Académico)

Lo que Académico (Leonel) necesita de Pagos para **gastar y devolver las horas** que los estudiantes compran
([CU26 a CU31](../CasosDeUso/CU26%20-%20Realizar%20pago.md)). La billetera vive en Pagos
([`DISENO_BASE_DE_DATOS.md`](DISENO_BASE_DE_DATOS.md#4-pagos--postgresql)); Académico nunca lee su base: usa este
contrato. **Es una propuesta para acordar con Leonel** antes de implementar. El formato exacto (rutas, campos,
códigos de error) lo fijan las UT de Pagos.

## Quién hace qué

| Pagos | Académico |
| --- | --- |
| Acredita horas al confirmar un pago (CU27) | Decide cuándo una clase gasta horas |
| Guarda el saldo y el libro de movimientos | Pide descontar o devolver horas |
| Descuenta de las compras más antiguas primero | No sabe de compras ni de dinero |
| Aparta las horas de un reembolso en curso | Recibe un rechazo si no hay horas disponibles |

## Operaciones

Todas por REST interno (servicio a servicio), con el código del estudiante y de su escuela.

1. **Consultar saldo:** devuelve las horas disponibles del estudiante. Sirve para saber si puede reservar.
2. **Descontar horas (consumo):** al reservar o dar una clase. Lleva el estudiante, las horas, el
   `instructor_codigo`, el `clase_codigo` y una **clave de idempotencia**.
   - Sin horas disponibles suficientes: se rechaza y no se descuenta nada.
   - Con la misma clave otra vez: devuelve el resultado anterior sin descontar de nuevo.
3. **Devolver horas:** si la clase se cancela. Lleva el `clase_codigo` y su propia clave de idempotencia; devuelve
   exactamente lo que ese consumo descontó.

## Reglas que Académico tiene que respetar

- **El `instructor_codigo` es obligatorio en cada consumo:** de ahí sale cuántas horas gastó cada alumno con cada
  instructor (CU29). Sin él, esa pantalla no puede mostrar nada.
- **Siempre con clave de idempotencia:** un reintento por mala red no puede descontar dos veces.
- **Horas enteras:** no hay horas fraccionarias.
- **Las horas apartadas por un reembolso no están disponibles:** si un reembolso está en curso, esas horas ya no
  se pueden gastar (el descuento falla como si no hubiera saldo).
- **Una compra con una sola hora gastada no se puede reembolsar** (CU28/CU31): gastar horas es lo que cierra esa
  posibilidad.
- **Las horas no vencen.**

## Pendiente de acordar con Leonel

- Si la reserva (CU14/CU44) descuenta las horas al reservar o al dar la clase, y qué pasa con el diseño de la saga
  reserva-pago ([SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md)): con la billetera, "pagar una reserva" pasa a ser
  "descontar horas". Puede cambiar el CU44.
- Por REST síncrono o por eventos (RabbitMQ) para devolver horas de una clase cancelada.
- Qué hace Académico cuando Pagos no responde al descontar (reintentar con la misma clave o fallar la reserva).
