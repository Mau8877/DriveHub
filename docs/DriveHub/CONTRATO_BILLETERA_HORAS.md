# Contrato de la billetera de horas (Pagos ↔ Académico)

Lo que Académico (Leonel) necesita de Pagos para **gastar y devolver las horas** que los estudiantes compran
([CU26 a CU31](../CasosDeUso/CU26%20-%20Realizar%20pago.md)). La billetera vive en Pagos
([`DISENO_BASE_DE_DATOS.md`](DISENO_BASE_DE_DATOS.md#4-pagos--postgresql)); Académico nunca lee su base: usa este
contrato. **Las rutas y los campos de abajo ya están implementados en Pagos (CU27)**; lo que falta acordar con Leonel está
al final.

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

## Rutas (por el gateway, con el JWT del usuario)

La escuela sale siempre del token. Todas responden con el sobre estándar.

| Operación | Ruta | Quién | Respuesta |
| --- | --- | --- | --- |
| Saldo propio | `GET /api/v1/billeteras-horas/mia` | `ESTUDIANTE` | `{ horasDisponibles }` |
| Saldo de un estudiante | `GET /api/v1/billeteras-horas/{estudianteCodigo}/saldo` | `INSTRUCTOR`, `ADMIN_ESCUELA` | `{ horasDisponibles }` |
| Descontar | `POST /api/v1/billeteras-horas/consumos` | `INSTRUCTOR`, `ADMIN_ESCUELA`; el `ESTUDIANTE` solo sobre sí | `201` `{ horasDescontadas, horasDisponibles }` |
| Devolver | `POST /api/v1/billeteras-horas/devoluciones` | igual que descontar | `200` `{ horasDevueltas, horasDisponibles }` |

- Descontar y devolver llevan el header `Idempotency-Key` (obligatorio, hasta 64 caracteres; sin él `ERR_SYS_01`).
- Cuerpo de descontar: `{ estudianteCodigo, horas (1 a 500), instructorCodigo, claseCodigo }`. Cuerpo de devolver:
  `{ estudianteCodigo, claseCodigo }`.
- Errores: `409 ERR_PAG_09` sin horas suficientes; `404 ERR_PAG_16` si esa clase no descontó horas.

### Rutas internas (CU44): sin sesión de usuario

El orquestador de Académico corre en un proceso programado, sin ningún estudiante conectado, así que no tiene un
Bearer. Para eso Pagos expone dos rutas **internas**, equivalentes a descontar y devolver:

| Operación | Ruta | Quién | Respuesta |
| --- | --- | --- | --- |
| Descontar | `POST /internal/v1/billeteras-horas/consumos` | Académico (credencial de servicio) | `201` `{ horasDescontadas, horasDisponibles }` |
| Devolver | `POST /internal/v1/billeteras-horas/devoluciones` | Académico (credencial de servicio) | `200` `{ horasDevueltas, horasDisponibles }` |

- Se identifican con el header **`X-Service-Key`** (un secreto compartido, variable `PAGOS_CREDENCIAL_ACADEMICO` en
  los dos servicios); sin él o con otro valor responden `401 ERR_PAG_19`. No llevan `Authorization`.
- Como no hay token, el cuerpo trae también la escuela: descontar `{ escuelaCodigo, estudianteCodigo, horas,
  instructorCodigo, claseCodigo }`; devolver `{ escuelaCodigo, estudianteCodigo, claseCodigo }`.
- Mismas reglas que las públicas: `Idempotency-Key` obligatoria, mismos errores (`ERR_PAG_09`, `ERR_PAG_16`) y misma
  idempotencia. Descontar o devolver por una ruta y repetirlo por la otra con la misma clave da el mismo resultado.
- **No pasan por el gateway** y no se registran en él: el navegador y la app nunca las alcanzan. Las rutas
  `/api/v1/billeteras-horas/*` de arriba no cambian.

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

## Acordado con Leonel (CU14)

> Explicación completa, con ejemplos: [FLUJO_PAGOS_HORAS_Y_RESERVAS.md](FLUJO_PAGOS_HORAS_Y_RESERVAS.md).

- **Cuándo se descuenta:** al reservar la clase, no al darla. `claseCodigo` es el código de la reserva y la clave
  de idempotencia es `reserva-{codigoReserva}` (la devolución usa `devolucion-{codigoReserva}`).
- **Cómo:** por REST síncrono, con el Bearer del estudiante. La devolución de CU15 usará el mismo mecanismo.
- **Si Pagos no responde al descontar:** la reserva falla para esa franja, que queda libre; Académico pide devolver
  la hora por si el descuento sí llegó a ocurrir (un `404 ERR_PAG_16` significa que no había nada que devolver).
  Si tampoco puede devolver, la reserva queda a medias y se retoma repitiendo el descuento con la misma clave.
- **Cuántas horas cuesta una clase:** 1, sin importar la duración de la franja (`app.reservas.horas-por-clase`).

## Todavía pendiente

- Qué pasa con el CU44 (saga reserva-pago, [SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md)) ahora que reservar es
  descontar horas: lo decide Mauro, dueño del CU44.
- Si CU15 devuelve las horas por REST síncrono (como se hace ahora) o por eventos (RabbitMQ).
