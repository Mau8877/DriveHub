# Unidades de trabajo — CU44-BE

**Caso de uso:** Orquestar clases y horas (saga): un proceso programado de Académico retoma las reservas que quedaron a medias entre la franja (Académico) y la hora de la billetera (Pagos), sin esperar al estudiante y sin su sesión.
**Microservicio(s):** `pagos` (UT 01 a 03, de Mauro), `gateway` (UT 04), `infra` (UT 05) y `academico` (UT 06 a 11, se coordinan con Leonel, dueño del servicio); UT 12 y 13 verifican y documentan todo — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md).
**Fuente CU:** [`CU44 - Orquestar clases y horas`](../../../CasosDeUso/CU44%20-%20Orquestar%20clases%20y%20horas.md)
**Recurso HTTP / contrato:** `POST /internal/v1/billeteras-horas/consumos` y `POST /internal/v1/billeteras-horas/devoluciones` (Pagos, solo con la credencial de servicio, **fuera del gateway**). Académico no expone nada nuevo: el job no tiene endpoint.

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `pagos` y `academico`), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [CONVENCIONES_GIT.md](../../../../.claude/rules/CONVENCIONES_GIT.md). Y el contrato de la billetera: [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) y el diseño de la saga: [SAGA_RESERVA_PAGO.md](../../../DriveHub/SAGA_RESERVA_PAGO.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Alcance: solo la reserva a medias (flujo S1 del CU).** Las cancelaciones y reprogramaciones interrumpidas (S2, S3) necesitan los estados y campos de CU15, que todavía no está documentado. Una UT no construye trabajo de otro CU, así que S2 y S3 quedan como follow-up: cuando exista CU15 se agregan sus UT, que **editan** el orquestador de la UT 09 en vez de encontrarlo ya hecho.
2. **El orquestador vive en Académico, sin servicio nuevo ni `SagaEstado`.** Es una clase (`OrquestadorClases`) más un job (`OrquestadorClasesJob`) dentro del módulo `reservas`. El estado de cada operación es el de la reserva.
3. **Credencial de servicio, no JWT.** El job corre sin ningún estudiante, así que no hay Bearer que reenviar. Académico se identifica ante Pagos con un **secreto compartido** en el header `X-Service-Key`. Un secreto por par de servicios (Académico→Pagos), por variable de entorno `PAGOS_CREDENCIAL_ACADEMICO`, comparado en tiempo constante. Si Pagos no tiene el secreto configurado, **rechaza todo** (falla cerrado), nunca lo deja abierto.
4. **Endpoints internos en Pagos, fuera de `/api/v1` y fuera del gateway.** Prefijo `/internal/v1/billeteras-horas`. Como el gateway enruta solo por `Path=` de recursos conocidos, no hay que "cerrar" nada: basta con **no registrarlo**, y la UT 04 deja un test que lo garantiza.
5. **El endpoint interno recibe `estudianteCodigo` y `escuelaCodigo` explícitos** (la billetera es por escuela y estudiante). Los dos salen de datos propios de Académico: `reserva.estudianteCodigo` y `franja.escuelaCodigo` (la reserva no guarda la escuela). Si la franja ya no existe, no se puede saber la escuela: cuenta como intento fallido y termina en revisión.
6. **Pagos suma operaciones "de sistema" al service, sin tocar las existentes.** `consumir` y `devolver` llevan `@PreAuthorize` (roles de sesión); el endpoint interno no tiene sesión. Se agregan `consumirDeSistema` y `devolverDeSistema`, que comparten el mismo núcleo, sin `@PreAuthorize` y con `@Transactional`.
7. **Mismas claves de idempotencia y mismo umbral que CU14.** `reserva-{codigoReserva}` para descontar, `devolucion-{codigoReserva}` para devolver (constantes ya definidas en `ReservaService`), y `app.reservas.minutos-para-retomar` (2). Así el job y el retomar que hace el estudiante al consultar sus clases nunca descuentan dos veces: si corren a la vez, el cambio de estado `PENDIENTE_PAGO → …` es atómico y el segundo no encuentra nada que cambiar.
8. **Una sola instancia de Académico.** El job no usa bloqueo distribuido (mismo criterio que el rate limit del gateway: no se anticipa hasta que haya más de una réplica).
9. **Revisión manual (A5), solo por causas propias.** Dos campos nuevos en `reservas`: `intentosRetomar` y `requiereRevision`. Tras `app.orquestador.maximo-intentos` (5) pasadas fallidas **por causas propias de Académico** (un dato inconsistente, una excepción inesperada) la reserva se marca y deja de retomarse. **Que Pagos no responda, o rechace la credencial, no cuenta como intento** (CU44 A1): una caída de Pagos de cualquier duración no puede dejar reservas abandonadas. Esos casos se reintentan en cada pasada y, si es la credencial, se registra un `ERROR` en cada una. No hay endpoint ni pantalla para listar las reservas en revisión: queda visible en el `WARN` del log y en el campo (follow-up).
10. **"Hora descontada sin reserva" (CU, S1.5) no puede ocurrir con el orden de CU14**, porque la reserva se crea `PENDIENTE_PAGO` **antes** de descontar. El caso equivalente real es "la franja ya no está apartada": se devuelve la hora y se cancela. La UT 09 cubre ese caso y el CA02 se verifica con él.
11. **No se tocan `ReservaService` ni `PagosClient`.** La lógica de retomar queda duplicada a propósito entre el service (con Bearer) y el orquestador (con credencial); unificarlas es un follow-up, porque refactorizar CU14 es otro cambio con su propio riesgo. Lo que sí queda **compartible desde el inicio** es la operación de deshacer (decisión 14).
12. **Un solo código de error nuevo:** `ERR_PAG_19` (401). Académico no devuelve errores nuevos: el job no responde a ningún cliente.

13. **Completar si es reciente, deshacer si es vieja (CU44 S1).** Una reserva a medias que lleva menos de `app.orquestador.minutos-para-deshacer` (por defecto `60`) se **completa**: se repite el descuento con la misma clave y se confirma. Una que lleva ese tiempo o más se **deshace**: se devuelve la hora (un `ERR_PAG_16` no es error), se libera la franja y se cancela, sin intentar completarla. Una franja que ya no está apartada para esa reserva también se deshace, sin importar su edad. Así el estudiante no recibe confirmada una clase que ya no espera. Esa propiedad vive en `OrquestadorProperties`, que **crea la UT 09** y las UT 10 y 11 amplían.
14. **«Deshacer una reserva» es una pieza compartida, pensada para CU15.** `OrquestadorClases.deshacerReserva(reserva, motivo)` hace las tres cosas de deshacer (devolver la hora con `devolucion-{codigoReserva}`, liberar la franja, `PENDIENTE_PAGO → CANCELADA`) y es **pública**: la usa el orquestador para las reservas viejas y la usará CU15 cuando una clase se cancele sin que el estudiante esté presente (instructor, Administrador o sistema). Cuando es el propio estudiante quien cancela, CU15 puede seguir usando su Bearer con `PagosClient`; las rutas internas sirven para lo demás. **Reprogramar no toca las horas**: se mueve la reserva de franja sin pasar por Pagos.
15. **Académico se modifica en tres momentos**: reservar (CU14, hecho, no cambia), retomar reservas a medias (esta UT, 06 a 11) y cancelar o reprogramar (CU15, follow-up S2 y S3). Pagos se modifica una sola vez (UT 01 a 03), para aceptar la credencial de servicio.
## Contrato de datos / recurso HTTP

Solo en Pagos. Todas las rutas exigen el header `X-Service-Key` con la credencial de servicio (`401 ERR_PAG_19` si falta o no coincide) y **no** llevan JWT. Respuestas con el sobre estándar. El header `Idempotency-Key` es obligatorio igual que en las rutas públicas de la billetera.

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /internal/v1/billeteras-horas/consumos` | `Idempotency-Key`, cuerpo `{ escuelaCodigo, estudianteCodigo, horas, instructorCodigo, claseCodigo }` | `201`, `data`: `ConsumoResultado` `{ horasDescontadas, horasDisponibles }` (`409 ERR_PAG_09` sin horas) |
| `POST /internal/v1/billeteras-horas/devoluciones` | `Idempotency-Key`, cuerpo `{ escuelaCodigo, estudianteCodigo, claseCodigo }` | `200`, `data`: `DevolucionResultado` (`404 ERR_PAG_16` si esa clase no gastó nada) |

### Errores nuevos (`ERR_PAG_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_PAG_19` | 401 | ninguna de negocio (la escribe el filtro) | Falta el header `X-Service-Key`, no coincide con la credencial configurada, o Pagos no tiene credencial configurada |

### Propiedades nuevas (Académico)

`app.pagos.credencial-servicio` (UT 06), `app.orquestador.minutos-para-deshacer` (UT 09, por defecto 60), `app.orquestador.maximo-intentos` (UT 10), `app.orquestador.habilitado`, `app.orquestador.cron` y `app.orquestador.lote` (UT 11). Y en Pagos `app.internal.credencial-academico` (UT 01).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU44-BE-01](./UT-CU44-BE-01.md) | Pagos: filtro de credencial de servicio y `ERR_PAG_19` | — |
| [UT-CU44-BE-02](./UT-CU44-BE-02.md) | Pagos: descontar y devolver horas sin sesión | — |
| [UT-CU44-BE-03](./UT-CU44-BE-03.md) | Pagos: endpoints internos de la billetera | 01, 02 |
| [UT-CU44-BE-04](./UT-CU44-BE-04.md) | Gateway: test de que no enruta `/internal` | — |
| [UT-CU44-BE-05](./UT-CU44-BE-05.md) | Infra: variable de la credencial de servicio | — |
| [UT-CU44-BE-06](./UT-CU44-BE-06.md) | Académico: cliente de Pagos con credencial de servicio | 03 |
| [UT-CU44-BE-07](./UT-CU44-BE-07.md) | Académico: campos de revisión en la reserva | — |
| [UT-CU44-BE-08](./UT-CU44-BE-08.md) | Académico: consultas del orquestador en el repositorio | 07 |
| [UT-CU44-BE-09](./UT-CU44-BE-09.md) | Académico: `OrquestadorClases`, completar o deshacer una reserva | 06, 08 |
| [UT-CU44-BE-10](./UT-CU44-BE-10.md) | Académico: intentos (sin contar a Pagos caído) y revisión manual | 09 |
| [UT-CU44-BE-11](./UT-CU44-BE-11.md) | Académico: job programado | 10 |
| [UT-CU44-BE-12](./UT-CU44-BE-12.md) | Prueba de punta a punta | 03, 04, 05, 11 |
| [UT-CU44-BE-13](./UT-CU44-BE-13.md) | Documentación | 12 |

**Cadena:** `01, 02, 04, 05, 07` son independientes → `03` (01, 02) y `08` (07) → `06` (03) → `09` (06, 08) → `10` → `11` → `12` (03, 04, 05, 11) → `13`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 a 5 | El job busca operaciones sin terminar, las completa o compensa y registra | 08, 09, 11 |
| S1 | Reserva interrumpida (descontar y confirmar, o liberar y cancelar) | 02, 03, 06, 09 |
| F1, CA11 | Deshacer una reserva vieja: devolver la hora, liberar la franja y cancelar | 09 |
| A1, CA07, CA12 | Pagos no responde o rechaza la credencial: se reintenta en la siguiente pasada y no cuenta como intento fallido | 06, 09, 10 |
| A2 | La franja ya fue tomada o liberada: devolver la hora y cancelar | 09 |
| A3 | `404 ERR_PAG_16` al devolver: no hay nada que devolver | 06, 09 |
| A5, CA08 | Sin resolverse tras varias pasadas por una causa propia: revisión manual | 07, 08, 10 |
| CA01, CA02, CA06, CA09, CA10, CA13 | Criterios de aceptación aplicables a la reserva y a la credencial | 01 a 04, 09 a 12 |
| S2, S3, A4, CA03, CA04, CA05 | Cancelación y reprogramación interrumpidas | **Follow-up** (dependen de CU15) |

## Pendiente para cuando exista el CU15 (hay que escribir UT nuevas)

Estas 13 UT dejan resuelto **solo** el flujo S1 (reserva a medias). **El CU44 no se da por terminado con ellas**: su estado queda `En progreso` (UT 13) hasta completar S2 y S3. Cuando el CU15 esté documentado y sus UT hechas, se agregan estas UT al CU44 (numeración desde `UT-CU44-BE-14`; no se renumeran las actuales):

| UT futura | Qué hace | Edita |
|-----------|----------|-------|
| `UT-CU44-BE-14` | Retomar una **cancelación** interrumpida (S2): clase cancelada con la hora sin devolver, o hora devuelta con la clase todavía activa. Usa `PagosServicioClient.devolver` y `OrquestadorClases.deshacerReserva`, que ya existen | `OrquestadorClases` (UT 09) |
| `UT-CU44-BE-15` | Retomar una **reprogramación** interrumpida (S3): franja nueva apartada y clase sin cambiar, o anterior liberada sin completar el cambio. No toca horas | `OrquestadorClases` y repositorio de franjas |
| `UT-CU44-BE-16` | Que el job y el contador de intentos (UT 08, 10 y 11) incluyan los estados nuevos de cancelar y reprogramar | `ReservaRepositoryCustom`, `OrquestadorClasesJob` |
| `UT-CU44-BE-17` | Ampliar la prueba de punta a punta con los escenarios de S2 y S3 | solo pruebas y notas |

Los criterios que quedan por cubrir son **CA03, CA04 y CA05** y los flujos **S2, S3 y A4**. Los nombres y el reparto exacto de esas UT se ajustan al diseño real del CU15 (sus estados y campos todavía no existen). **No hay que rehacer nada** de lo ya hecho: el secreto compartido, los endpoints internos y el cliente (UT 01 a 06) y la revisión manual (UT 07, 08 y 10) sirven tal cual.

## Reglas de negocio de CU15 pendientes de definir

Afectan cómo se usa `deshacerReserva` y los estados de cancelar y reprogramar; se definen al documentar CU15, no antes:

- Plazo para cancelar con devolución de la hora y qué pasa con la hora en una cancelación tardía.
- Quién puede cancelar (estudiante, instructor, Administrador) y qué pasa con la hora en cada caso.
- Cuántas veces y con qué anticipación se puede reprogramar.
- Qué pasa con la hora del estudiante que reservó y no asiste (hoy se asume que la pierde; ver CU44, reglas de negocio).

## Follow-ups (no se implementan en estas UT)

- **S2 y S3**: ver la sección anterior.
- **Unificar** la lógica de retomar de `ReservaService` y la de `OrquestadorClases` en una sola.
- **Ver las reservas en revisión** (listado o aviso al Administrador): hoy solo en el log y en el campo `requiereRevision`.
- **Bloqueo distribuido del job** si Académico pasa a más de una réplica.

## Qué NO incluyen estas UT

- Cancelar o reprogramar una clase (CU15) ni sus estados.
- Cambios en el flujo en vivo del estudiante: `ReservaService`, `PagosClient` y `ReservaController` no se tocan.
- Cambios en la app móvil o en la web: el job no tiene pantalla.
- Eventos de RabbitMQ ni la tabla `SagaEstado`: no se usan.
- Registrar los endpoints internos en el gateway: **está prohibido**.
