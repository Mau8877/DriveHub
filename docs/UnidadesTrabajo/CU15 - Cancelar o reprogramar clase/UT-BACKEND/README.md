# Unidades de trabajo — CU15-BE

**Caso de uso:** Cancelar o reprogramar clase: el estudiante cancela una clase reservada (con la hora devuelta si faltan 12 horas o más, perdida si falta menos) o la mueve a otra franja libre sin tocar sus horas; si algo se interrumpe, el orquestador de CU44 lo completa solo.
**Microservicio(s):** `academico` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Pagos, Identidad y el gateway **no cambian**: Pagos ya devuelve horas (`POST /billeteras-horas/devoluciones`, CU27, y su ruta interna de CU44) y el gateway ya enruta `/api/v1/reservas/**`.
**Fuente CU:** [`CU15 - Cancelar o reprogramar clase`](../../../CasosDeUso/CU15%20-%20Cancelar%20o%20reprogramar%20clase.md)
**Recurso HTTP / contrato:** `POST /api/v1/reservas/{codigo}/cancelar` y `POST /api/v1/reservas/{codigo}/reprogramar` (Académico, solo rol `ESTUDIANTE`)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `academico`), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query; todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (acciones como sub-recurso verbo), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y la guía de CU44 «[Contrato para CU15](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md#contrato-para-cu15-cómo-se-enchufa-guía-para-leonel)» y [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Estas UT son los «UT 17 y 18» que CU44 dejó como follow-up.** Los manejadores de cancelación y de reprogramación (UT 05 y 06) viven acá, en el CU que los necesita, no en CU44. La UT 10 deja anotado en el README de CU44 que están hechos.
2. **Dos estados transitorios nuevos en `EstadoReserva`:** `CANCELACION_PENDIENTE` («se pidió cancelar, la franja y/o la hora todavía no se resolvieron») y `REPROGRAMACION_PENDIENTE` («se eligió otra franja, el cambio no terminó»). Cada uno tiene su manejador (`ManejadorDeOperacion`), así que el job, el contador de intentos y la revisión manual de CU44 los cubren sin tocar el núcleo.
3. **Campos nuevos en `reservas`:** `franjaDestinoId` (`Long`, solo durante una reprogramación) y `sinDevolucion` (`boolean`, `true` si la cancelación es tardía y la hora no se devuelve). Una reserva vieja sin los campos lee `null` y `false`.
4. **Cancelar, a tiempo o tardía, usa siempre el mismo camino:** la reserva pasa de `CONFIRMADA` a `CANCELACION_PENDIENTE` (con `sinDevolucion` según el plazo) y el `ManejadorDeCancelacion` la termina: si `sinDevolucion` es `false` devuelve la hora; en los dos casos libera la franja y deja `CANCELADA`. Así una tardía interrumpida también se completa (si no, la franja podría quedar apartada por una reserva cancelada).
5. **Pagos no sabe de plazos:** la regla «a tiempo / tardía» se decide en Académico **antes** de marcar `CANCELACION_PENDIENTE`. Una cancelación tardía no llama a Pagos.
6. **Plazo:** `app.reservas.horas-para-cancelar=12`, configuración del servicio (no un dato de la clase), medido contra el `Clock` inyectado y la zona `app.horarios.zona-horaria` (ya existe). Con 12 horas o más es «a tiempo» (el límite exacto cuenta como a tiempo). Misma propiedad para reprogramar.
7. **`CompensadorDeReserva` se generaliza (UT 04):** hoy solo cancela desde `PENDIENTE_PAGO`. Pasa a recibir el estado de origen y si hay que devolver la hora, con los valores de hoy como defecto: lo de CU14 y CU44 no cambia. Es el único archivo de CU44 que estas UT editan, y la UT 10 lo anota.
8. **Reprogramar sin Pagos, y con un orden que no deja a nadie sin franja ni con dos:** (1) `CONFIRMADA → REPROGRAMACION_PENDIENTE` guardando `franjaDestinoId`; (2) apartar la franja destino; si falla, volver a `CONFIRMADA` y limpiar el destino; (3) liberar la franja anterior (solo si la franja destino ya es de la reserva); (4) mover la reserva a la franja destino (fecha, horas, instructor) y volver a `CONFIRMADA`. Cualquier corte se reanuda en el paso que falta: si la destino es de la reserva, se terminan 3 y 4; si sigue libre, se reintenta apartar y, si otro estudiante la tomó, se vuelve a `CONFIRMADA` en la franja original (que sigue siendo suya porque solo se libera después de asegurar la nueva).
9. **La antigüedad de una reserva a medias se mide por `updatedAt`, no por `createdAt` (UT 01):** una clase confirmada hace una semana tiene `createdAt` viejo; con `createdAt` el job tomaría cada cancelación recién pedida al instante y competiría con la petición del estudiante. `findAndModify` no actualiza `updatedAt` solo, así que `cambiarEstado` y las operaciones nuevas lo fijan. Para `PENDIENTE_PAGO` es equivalente a lo de hoy (`updatedAt == createdAt`).
10. **Qué ve el estudiante mientras algo está a medias:** `CANCELACION_PENDIENTE` no aparece en «Mis clases» ni en «Historial» (ya la canceló; sale de ahí). `REPROGRAMACION_PENDIENTE` **sí** aparece en «Mis clases», con la franja anterior (todavía es suya), y bloquea solapes. El historial ya muestra las `CANCELADA` que alguna vez estuvieron `CONFIRMADA`, así que una cancelada entra sola.
11. **Cancelar con Pagos caído acepta la cancelación:** responde `200` con `horaDevuelta = EN_PROCESO`; el job devuelve la hora cuando Pagos vuelva. El saldo final sale de `GET /billeteras-horas/mia` y, si esa lectura falla, `horasDisponibles` va en `null` (como en CU14).
12. **Errores nuevos (siguiente libre `ERR_ACA_32`):** `ERR_ACA_32` (404, la reserva no existe o no es del estudiante), `ERR_ACA_33` (409, la reserva no se puede cancelar o reprogramar: no está `CONFIRMADA` o ya empezó) y `ERR_ACA_34` (409, reprogramar fuera de plazo). Para la franja destino se reutilizan `ERR_ACA_10`, `ERR_ACA_18` y `ERR_ACA_20`. Cada código se documenta en `docs/errors/academico/` en la UT que lo introduce.
13. **`cancelacionATiempo` en `ReservaResponse`:** un booleano que calcula el servidor con su reloj (¿falta el plazo o más?). La app lo usa para el aviso «se devolverá / perderás la hora» y para ofrecer «Reprogramar» sin repetir el 12 en el teléfono.

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `ESTUDIANTE` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol, `403 ERR_ACA_16` si el token no trae escuela). Respuestas con el sobre estándar. La reserva se identifica por su `codigo` en la URL; el estudiante y su escuela salen del token.

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /api/v1/reservas/{codigo}/cancelar` | — (sin cuerpo) | `200`, `data`: `CancelacionResponse`, «Clase cancelada correctamente» |
| `POST /api/v1/reservas/{codigo}/reprogramar` | `ReprogramarReservaRequest` | `200`, `data`: `ReservaResponse` (ya con la franja nueva), «Clase reprogramada correctamente» |

`ReprogramarReservaRequest`: `franjaCodigo` (UUID, obligatorio).

`CancelacionResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `reserva` | `ReservaResponse` | La clase ya cancelada (`estado: CANCELADA`) o, con Pagos caído, aún sin devolver |
| `horaDevuelta` | `DEVUELTA`, `PERDIDA` o `EN_PROCESO` | `DEVUELTA`: la hora volvió a la billetera. `PERDIDA`: cancelación tardía. `EN_PROCESO`: Pagos no respondió, el sistema la devuelve solo |
| `horasDisponibles` | int o null | Saldo real tras cancelar; `null` si Pagos no respondió |

`ReservaResponse` gana un campo: `cancelacionATiempo` (boolean).

### Errores (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_32` | 404 | `NotFoundException` | La reserva no existe, está borrada o es de otro estudiante |
| `ERR_ACA_33` | 409 | `ConflictException` | La reserva no está `CONFIRMADA` (ya cancelada, completada, a medias...) o ya empezó |
| `ERR_ACA_34` | 409 | `ConflictException` | Reprogramar con menos de 12 horas de anticipación |

Se reutilizan: `ERR_ACA_10` (la franja destino ya empezó o pasó), `ERR_ACA_18` (la franja destino ya no está libre, no existe o es de otra escuela), `ERR_ACA_20` (el estudiante ya tiene otra clase a esa fecha y hora), `ERR_ACA_16` y `ERR_ACA_17`. Reprogramar a la franja en la que ya está la clase es `400 ERR_SYS_01`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU15-BE-01](./UT-CU15-BE-01.md) | Estados y campos de la reserva; cancelación y antigüedad en el repositorio | — |
| [UT-CU15-BE-02](./UT-CU15-BE-02.md) | Repositorio: reprogramación, solape y «Mis clases» | 01 |
| [UT-CU15-BE-03](./UT-CU15-BE-03.md) | Errores, plazo configurable y regla de «a tiempo» | — |
| [UT-CU15-BE-04](./UT-CU15-BE-04.md) | Generalizar `CompensadorDeReserva` | 01 |
| [UT-CU15-BE-05](./UT-CU15-BE-05.md) | `ManejadorDeCancelacion` | 01, 04 |
| [UT-CU15-BE-06](./UT-CU15-BE-06.md) | `ManejadorDeReprogramacion` | 02 |
| [UT-CU15-BE-07](./UT-CU15-BE-07.md) | `ReservaService`: cancelar | 03, 05 |
| [UT-CU15-BE-08](./UT-CU15-BE-08.md) | `ReservaService`: reprogramar | 02, 03, 06 |
| [UT-CU15-BE-09](./UT-CU15-BE-09.md) | Endpoints de cancelar y reprogramar | 07, 08 |
| [UT-CU15-BE-10](./UT-CU15-BE-10.md) | Prueba de punta a punta y documentación | 09 |

**Cadena:** `01` y `03` son independientes → `02` (01), `04` (01) → `05` (01, 04) y `06` (02) → `07` (03, 05) y `08` (02, 03, 06) → `09` → `10`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 4 y 5 | Cancelar a tiempo: liberar la franja, devolver la hora, informar | 01, 03, 04, 05, 07, 09 |
| A8 | Cancelación tardía: se cancela y no se devuelve la hora | 01, 03, 05, 07 |
| F1 | Reprogramar a otra franja libre | 02, 03, 06, 08, 09 |
| S1 | Terminar una cancelación o reprogramación interrumpida | 01, 05, 06 |
| A2, A3 | Franja destino ocupada, ya empezada o solapada | 02, 08 |
| A4, A5, A6 | Fuera de plazo, clase no cancelable, clase ajena | 03, 07, 08 |
| A7 | Pagos no responde al devolver: se acepta y se devuelve sola | 05, 07 |
| A9 | Rol, sesión y escuela | 09 |
| CA01 a CA16 | Criterios de aceptación (CA03 retirado) | 05 a 10 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 09 | **Hechas** (2026-11-01), con pruebas: `academico` 536 tests unitarios y 133 con MongoDB real (`ReservaRepositoryTest` 35, `ReservarHorarioIntegracionTest` 8...), sin fallos; `./mvnw checkstyle:check` limpio |
| 10 (punta a punta) | **Parcial.** Hechos con el stack real y sin sesión de estudiante (los mueve el proceso programado): cancelación a tiempo, tardía y con Pagos caído, reprogramación interrumpida (la franja destino ya es de la reserva, o la tomó otro) y que un cambio reciente no se toca antes de `minutos-para-retomar`. **Pendientes** (necesitan el JWT de un estudiante de `escuelaexample`, que no se probó porque no se dispone de su clave): los escenarios por HTTP 1, 2, 4, 5, 6 y 8, y la actualización final del CU |

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **Los tests de repositorio con MongoDB real se omiten solos en esta máquina** (`Tests run: 0`): Testcontainers no ve Docker Desktop. Se corrieron contra el Mongo del compose con una base temporal (`academico_ut_tmp`, borrada al terminar), parchando solo durante la corrida `MongoIntegrationTestBase`. El nombre real del test es `ReservaRepositoryTest`, no `ReservaRepositoryImplTest`.
- **Los DTOs se hicieron junto con los servicios (UT 07 a 09)**: `ReservaService.cancelar` y `reprogramar` devuelven DTO (`CancelacionResponse`, `ReservaResponse`), como `reservar`, porque unen el nombre del instructor. `HoraDeCancelacion` vive en `dto/`, no en `service/`.
- **El cliente nunca ve los estados transitorios:** `ReservaResponse` muestra `CANCELACION_PENDIENTE` como `CANCELADA` y `REPROGRAMACION_PENDIENTE` como `CONFIRMADA`.
- **`horasDisponibles` va `null` también cuando la hora está `EN_PROCESO`** (no solo si falla la lectura): el saldo aún no refleja la devolución y mostrarlo engañaría.
- **Reprogramar con Identidad caída no devuelve `ERR_ACA_17`:** la reprogramación ya está hecha, así que el instructor se muestra como «Instructor» (igual que `reservar`). Cambia la ficha de la UT 08.
- **`existeSolape` mantiene su versión de tres argumentos** como método `default` de la interfaz que delega con `null`; los tests de CU14 no cambiaron.
- **`pendientesAntiguas` ordena y filtra por `updatedAt`** (UT 01). `ManejadorDeReserva` sigue midiendo sus 60 minutos con `createdAt`.
- **`ReservasProperties` ganó `horasParaCancelar`** (cuarto componente del `record`): los tests que lo construyen a mano pasan `12`.
- **El `403` por rol no se prueba en el test de controller** (el `@PreAuthorize` está en el servicio, que ahí es un simulado): `ReservaServiceTest` comprueba por reflexión que `cancelar` y `reprogramar` llevan `hasRole('ESTUDIANTE')`.
- **Una cancelación con un fallo inesperado al terminar** (por ejemplo, Mongo caído) no se deshace: queda `CANCELACION_PENDIENTE`, la respuesta dice `EN_PROCESO` y el proceso programado la termina.
- **Resultados con el stack real (proceso programado, 2026-11-01):** con Pagos detenido, la cancelación a tiempo (R3, R4) esperó sin sumar intentos y, al volver Pagos, quedó `CANCELADA` con **una** devolución cada una (5 h → 5 h); la tardía (R5, R7) se canceló sin llamar a Pagos y sin devolver; la reprogramación con la destino ya apartada se movió y liberó la anterior; con la destino tomada por otra reserva volvió a `CONFIRMADA` en la original y no tocó la destino; una cancelación con `updatedAt` reciente no se tocó hasta pasar los 2 minutos aunque su `createdAt` tenía días. Datos temporales borrados.

## Follow-ups (no se implementan en estas UT)

- **Cancelación por el instructor o el Administrador:** CU16. Reutilizará `ManejadorDeCancelacion` (con `sinDevolucion = false`) y el cliente de servicio.
- **Avisar** a la otra persona de una cancelación o un cambio: CU37 y CU38.
- **Ver las reservas en revisión** (listado o aviso al Administrador): follow-up de CU44.
- **Retomar al consultar** («Mis clases») las cancelaciones y reprogramaciones a medias del propio estudiante, como CU14 hace con `PENDIENTE_PAGO`: hoy solo las retoma el proceso programado.

## Qué NO incluyen estas UT

- Cancelar clases por el instructor o el Administrador (CU16), cerrar la clase y la asistencia (CU17), el calendario (CU16).
- Cambios en Pagos, Identidad o el gateway.
- La app móvil: está en [`UT-MOBILE`](../UT-MOBILE/README.md).
- Eventos de RabbitMQ y notificaciones: no se publica nada.
- Reembolsos de dinero (CU28 y CU31).
