# Unidades de trabajo — CU44-BE

**Caso de uso:** Orquestar clases y horas (saga): ponerle al flujo de reservar (CU14) una **saga orquestadora** de Académico que lo coordina con la billetera de Pagos, que retoma sola lo que quede a medias (sin esperar al estudiante y sin su sesión) y que **queda lista para que CU15 (cancelar y reprogramar) se enchufe** sin reescribirla.
**Microservicio(s):** `pagos` (UT 01 a 03, de Mauro), `gateway` (UT 04), `infra` (UT 05) y `academico` (UT 06 a 11 y 14 a 16, se coordinan con Leonel, dueño del servicio); UT 12 y 13 verifican y documentan todo — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md).
**Fuente CU:** [`CU44 - Orquestar clases y horas`](../../../CasosDeUso/CU44%20-%20Orquestar%20clases%20y%20horas.md)
**Recurso HTTP / contrato:** `POST /internal/v1/billeteras-horas/consumos` y `POST /internal/v1/billeteras-horas/devoluciones` (Pagos, solo con la credencial de servicio, **fuera del gateway**). Académico no expone nada nuevo: el job no tiene endpoint.

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `pagos` y `academico`), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [CONVENCIONES_GIT.md](../../../../.claude/rules/CONVENCIONES_GIT.md). Y el contrato de la billetera: [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) y el diseño de la saga: [SAGA_RESERVA_PAGO.md](../../../DriveHub/SAGA_RESERVA_PAGO.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Qué se quiere lograr (los dos objetivos del CU44)

1. **Que el CU14 tenga una saga orquestadora.** Hoy el CU14 tiene su propia saga simple escrita dentro de `ReservaService`, y retomar una reserva a medias solo ocurre cuando el estudiante consulta sus clases. El CU44 la lleva a **una sola saga**, `OrquestadorClases`, que la usan el flujo en vivo de reservar, el retomar al consultar y el proceso programado (UT 09 a 11 y 16).
2. **Que CU15 se enchufe sin reescribir nada.** Cuando Leonel haga cancelar y reprogramar, solo agrega sus estados y un **manejador** que se registra en el orquestador, y el proceso programado ya lo retoma (UT 14 y 15). Esto se verifica con un test, no solo se explica.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Alcance de lo que se construye: el flujo S1 (reserva a medias) y la estructura para CU15.** Las cancelaciones y reprogramaciones interrumpidas (S2, S3) necesitan los estados y campos de CU15, que todavía no está documentado. Una UT no construye trabajo de otro CU, así que S2 y S3 quedan como follow-up: cuando exista CU15 se agrega su **manejador** (UT 17 y 18), sin editar el núcleo del orquestador.
2. **El orquestador vive en Académico, sin servicio nuevo ni `SagaEstado`.** Es una clase (`OrquestadorClases`) más un job (`OrquestadorClasesJob`) dentro del módulo `reservas`. El estado de cada operación es el de la reserva.
3. **Credencial de servicio, no JWT.** El job corre sin ningún estudiante, así que no hay Bearer que reenviar. Académico se identifica ante Pagos con un **secreto compartido** en el header `X-Service-Key`. Un secreto por par de servicios (Académico→Pagos), por variable de entorno `PAGOS_CREDENCIAL_ACADEMICO`, comparado en tiempo constante. Si Pagos no tiene el secreto configurado, **rechaza todo** (falla cerrado), nunca lo deja abierto.
4. **Endpoints internos en Pagos, fuera de `/api/v1` y fuera del gateway.** Prefijo `/internal/v1/billeteras-horas`. Como el gateway enruta solo por `Path=` de recursos conocidos, no hay que "cerrar" nada: basta con **no registrarlo**, y la UT 04 deja un test que lo garantiza.
5. **El endpoint interno recibe `estudianteCodigo` y `escuelaCodigo` explícitos** (la billetera es por escuela y estudiante). Los dos salen de datos propios de Académico: `reserva.estudianteCodigo` y `franja.escuelaCodigo` (la reserva no guarda la escuela). Si la franja ya no existe, no se puede saber la escuela: cuenta como intento fallido propio y termina en revisión.
6. **Pagos suma operaciones "de sistema" al service, sin tocar las existentes.** `consumir` y `devolver` llevan `@PreAuthorize` (roles de sesión); el endpoint interno no tiene sesión. Se agregan `consumirDeSistema` y `devolverDeSistema`, que comparten el mismo núcleo, sin `@PreAuthorize` y con `@Transactional`.
7. **Mismas claves de idempotencia y mismo umbral que CU14.** `reserva-{codigoReserva}` para descontar, `devolucion-{codigoReserva}` para devolver (constantes ya definidas en `ReservaService`), y `app.reservas.minutos-para-retomar` (2). Así el job y el retomar del estudiante nunca descuentan dos veces: si corren a la vez, el cambio de estado es atómico y el segundo no encuentra nada que cambiar.
8. **Una sola instancia de Académico.** El job no usa bloqueo distribuido (mismo criterio que el rate limit del gateway: no se anticipa hasta que haya más de una réplica).
9. **Revisión manual (A5), solo por causas propias.** Dos campos nuevos en `reservas`: `intentosRetomar` y `requiereRevision`. Tras `app.orquestador.maximo-intentos` (5) pasadas fallidas **por causas propias de Académico** (un dato inconsistente, una excepción inesperada) la reserva se marca y deja de retomarse. **Que Pagos no responda, o rechace la credencial, no cuenta como intento** (CU44 A1): una caída de Pagos de cualquier duración no puede dejar reservas abandonadas. Esos casos se reintentan en cada pasada y, si es la credencial, se registra un `ERROR` en cada una. No hay endpoint ni pantalla para listar las reservas en revisión: queda visible en el `WARN` del log y en el campo (follow-up).
10. **"Hora descontada sin reserva" (CU, S1) no puede ocurrir con el orden de CU14**, porque la reserva se crea `PENDIENTE_PAGO` **antes** de descontar. El caso equivalente real es "la franja ya no está apartada": se devuelve la hora y se cancela. La UT 09 cubre ese caso.
11. **Una sola saga: el flujo en vivo del CU14 pasa a usar el orquestador (UT 16).** Hoy la misma lógica está escrita dos veces: en `ReservaService` (con el Bearer del estudiante) y, con el CU44, en el orquestador (con la credencial de servicio). La UT 16 hace que `ReservaService` **delegue** en `OrquestadorClases`. Va **al final y es una UT aparte** porque toca código de CU14 que ya funciona: las UT 01 a 15 no dependen de ella, y si se descarta, el sistema sigue funcionando con la lógica duplicada. Hasta que se haga, `ReservaService` y `PagosClient` **no se modifican**.
12. **Un solo código de error nuevo:** `ERR_PAG_19` (401). Académico no devuelve errores nuevos: el job no responde a ningún cliente.
13. **Completar si es reciente, deshacer si es vieja (CU44 S1).** Una reserva a medias que lleva menos de `app.orquestador.minutos-para-deshacer` (por defecto `60`) se **completa**: se repite el descuento con la misma clave y se confirma. Una que lleva ese tiempo o más se **deshace**: se devuelve la hora (un `ERR_PAG_16` no es error), se libera la franja y se cancela, sin intentar completarla. Una franja que ya no está apartada para esa reserva también se deshace, sin importar su edad. Así el estudiante no recibe confirmada una clase que ya no espera.
14. **La credencial se abstrae: `ClienteDePagos` (UT 14).** Una interfaz con `descontar` y `devolver` y **dos implementaciones**: una con el Bearer del estudiante (la saga en vivo, envolviendo al `PagosClient` existente **sin modificarlo**) y otra con la credencial de servicio (el job y las cancelaciones sin estudiante). El orquestador recibe el cliente que corresponda en cada llamada. Así «deshacer una reserva» y «devolver la hora» sirven igual cuando el estudiante está presente y cuando no, sin escribir dos caminos.
15. **Un punto de extensión para CU15: manejadores por estado transitorio (UT 09 y 15).** El orquestador no sabe de reservas ni de cancelaciones: recorre los **manejadores** registrados (`ManejadorDeOperacion`, un bean de Spring), cada uno declara qué estados transitorios atiende y sabe resolver una reserva en ese estado. En esta tanda hay uno solo, `ManejadorDeReserva` (`PENDIENTE_PAGO`, el flujo S1). Cuando exista CU15, agrega el suyo y **no se edita** el núcleo: el job, el contador de intentos y la revisión manual ya funcionan sobre todos los estados que declaren los manejadores. La UT 15 lo demuestra con un manejador de prueba.
16. **«Deshacer una reserva» es una pieza compartida, pensada para CU15.** `OrquestadorClases.deshacerReserva(reserva, motivo, cliente)` hace las tres cosas de deshacer (devolver la hora con `devolucion-{codigoReserva}`, liberar la franja, `…→ CANCELADA`) y es **pública**: la usa el manejador de reservas para las reservas viejas y la usará CU15 cuando una clase se cancele. **Reprogramar no toca las horas**: se mueve la reserva de franja sin pasar por Pagos.
17. **Académico se modifica en tres momentos**: reservar (CU14: hoy no cambia; con la UT 16 delega en el orquestador), retomar reservas a medias (UT 06 a 11 y 14) y cancelar o reprogramar (CU15, follow-up). Pagos se modifica una sola vez (UT 01 a 03), para aceptar la credencial de servicio.

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
| [UT-CU44-BE-14](./UT-CU44-BE-14.md) | Académico: `ClienteDePagos`, una interfaz con dos credenciales | 06 |
| [UT-CU44-BE-08](./UT-CU44-BE-08.md) | Académico: consultas del orquestador en el repositorio | 07 |
| [UT-CU44-BE-09](./UT-CU44-BE-09.md) | Académico: `OrquestadorClases`, manejadores y `ManejadorDeReserva` | 08, 14 |
| [UT-CU44-BE-10](./UT-CU44-BE-10.md) | Académico: intentos (sin contar a Pagos caído) y revisión manual | 09 |
| [UT-CU44-BE-11](./UT-CU44-BE-11.md) | Académico: job programado | 10 |
| [UT-CU44-BE-15](./UT-CU44-BE-15.md) | Académico: el punto de extensión para CU15, demostrado | 11 |
| [UT-CU44-BE-12](./UT-CU44-BE-12.md) | Prueba de punta a punta | 03, 04, 05, 15 |
| [UT-CU44-BE-13](./UT-CU44-BE-13.md) | Documentación | 12 |
| [UT-CU44-BE-16](./UT-CU44-BE-16.md) | Académico: el flujo en vivo del CU14 usa el orquestador (**opcional**, toca CU14) | 10, 14 (y 12 hecha) |

**Cadena:** `01, 02, 04, 05, 07` son independientes → `03` (01, 02) y `08` (07) → `06` (03) → `14` (06) → `09` (08, 14) → `10` → `11` → `15` → `12` (03, 04, 05, 15) → `13`. La `16` va **después** de la `12` y no bloquea nada.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 a 5 | El job busca operaciones sin terminar, las completa o compensa y registra | 08, 09, 11 |
| S1 | Reserva interrumpida (completar si es reciente) | 02, 03, 06, 09 |
| F1, CA11 | Deshacer una reserva vieja: devolver la hora, liberar la franja y cancelar | 09 |
| A1, CA07, CA12 | Pagos no responde o rechaza la credencial: se reintenta y no cuenta como intento fallido | 06, 09, 10 |
| A2, CA02 | La franja ya fue tomada o liberada: devolver la hora y cancelar | 09 |
| A3 | `404 ERR_PAG_16` al devolver: no hay nada que devolver | 06, 09 |
| A5, CA08 | Sin resolverse tras varias pasadas por una causa propia: revisión manual | 07, 08, 10 |
| A6, CA06 | El estudiante y el sistema retoman a la vez: una sola gana, sin cobro doble | 09, 12 |
| CA01, CA09, CA10, CA13 | Resolver sin sesión, no tocar `NO_ASISTIO`, credencial y gateway | 01 a 04, 09 a 12 |
| **Una sola saga para el CU14** | El flujo en vivo de reservar usa el mismo orquestador | 14, 16 |
| **CU15 enchufable** | Cancelar y reprogramar se agregan como manejadores sin editar el núcleo | 08, 09, 14, 15 |
| S2, S3, A4, CA03, CA04, CA05 | Cancelación y reprogramación interrumpidas | **Follow-up** (UT 17 y 18, dependen de CU15) |

## Contrato para CU15: cómo se enchufa (guía para Leonel)

El CU44 **no construye** cancelar ni reprogramar, pero deja el orquestador listo. Cuando Leonel haga el CU15, solo tiene que hacer esto; **no** hace falta editar `OrquestadorClases`, el job ni el contador de intentos.

### Qué ya existe (no se vuelve a escribir)

| Pieza | Dónde | Para qué la usa CU15 |
| --- | --- | --- |
| `OrquestadorClases` y `OrquestadorClasesJob` | Académico, `features/reservas/service/` (UT 09 y 11) | Recorren todos los estados transitorios que declaren los manejadores, cuentan intentos, marcan revisión y registran |
| `ManejadorDeOperacion` (interfaz) | Académico (UT 09) | **El punto de enchufe**: CU15 escribe un manejador por operación (cancelar, reprogramar) y lo declara como bean |
| `OrquestadorClases.deshacerReserva(reserva, motivo, cliente)` | Académico (UT 09) | Devolver la hora, liberar la franja y cancelar, todo junto y repetible. **Es la cancelación** |
| `ClienteDePagos` con sus dos implementaciones | Académico (UT 14) | Devolver la hora con el Bearer del estudiante (cancela él) o con la credencial de servicio (cancela el instructor, el Administrador o el sistema) |
| `POST /internal/v1/billeteras-horas/devoluciones` | Pagos (UT 03) | La ruta que usa el cliente de servicio |
| Clave `devolucion-{codigoReserva}` y `claseCodigo = codigoReserva` | CU14 | Pagos devuelve **exactamente** lo que esa clase consumió, y repetir la llamada no devuelve dos veces |

### Qué tiene que agregar CU15 (esto sí es trabajo de CU15)

1. **Los estados transitorios de la reserva** para cancelar y reprogramar (por ejemplo, «cancelación pedida, hora todavía sin devolver»). Sin un estado intermedio el orquestador no puede saber que una cancelación quedó a medias.
2. **Un `ManejadorDeOperacion` por operación**, declarado como bean de Spring, que dice qué estados atiende (`estados()`) y cómo resolver una reserva en cada uno (`resolver(reserva, cliente)`). Usa `deshacerReserva` y el `ClienteDePagos` recibido; no vuelve a escribir la devolución.
3. **Las reglas de negocio**, que se deciden **antes** de llamar a Pagos: Pagos no sabe de plazos y **siempre devuelve** lo que se le pide. Si la regla dice que una cancelación tardía pierde la hora, **Académico no llama a devolver**. Están pendientes: plazo de cancelación con devolución, cancelación tardía, quién puede cancelar y cuántas veces se puede reprogramar.
4. **El endpoint y la pantalla** de cancelar y reprogramar, con la decisión de qué cliente usar (Bearer del estudiante si cancela él; credencial de servicio en los demás casos).
5. **Reprogramar sin tocar Pagos:** primero se aparta la franja nueva, luego la reserva pasa a ella y recién después se libera la anterior. Las horas no cambian.

### Qué hay que cuidar

- **No copiar la lógica de devolver:** llamar a `deshacerReserva` o al cliente. Duplicarla es lo que provoca devoluciones dobles.
- **No cambiar las claves de idempotencia:** `reserva-{codigoReserva}` y `devolucion-{codigoReserva}`.
- **Un `404 ERR_PAG_16` al devolver no es un error:** significa que esa clase no tenía horas por devolver.
- **Si Pagos no responde, no se cancela por adivinanza:** el manejador devuelve `PAGOS_NO_RESPONDE` y la reserva queda en su estado intermedio; el orquestador la retoma sin contar intento.
- **Cómo se verifica que quedó enchufado:** el test plantilla es `OrquestadorClasesTest.unManejadorNuevo_recibeLasReservasDeSuEstado_conElClienteQueSePasa` (y `dosManejadoresConElMismoEstado_hacenFallarElArranque`). CU15 agrega uno igual con su manejador real.
- **Dónde está la lógica de deshacer:** en `CompensadorDeReserva` (bean aparte, para que `ManejadorDeReserva` y `OrquestadorClases` la compartan sin depender uno del otro). `OrquestadorClases.deshacerReserva` solo delega: es la puerta pública que usa CU15.
- Cuando CU15 esté documentado, se agregan las **UT 17 y 18** del CU44 (ver abajo): solo escriben el manejador y su prueba.

## Reglas de negocio de CU15 pendientes de definir

Afectan cómo se usa `deshacerReserva` y los estados de cancelar y reprogramar; se definen al documentar CU15, no antes:

- Plazo para cancelar con devolución de la hora y qué pasa con la hora en una cancelación tardía.
- Quién puede cancelar (estudiante, instructor, Administrador) y qué pasa con la hora en cada caso.
- Cuántas veces y con qué anticipación se puede reprogramar.
- Qué pasa con la hora del estudiante que reservó y no asiste (hoy se asume que la pierde; ver CU44, reglas de negocio).

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 03 (Pagos), 04 y 05 (gateway e infra) | Hechas |
| 06 y 14 (clientes de Pagos), 07 y 08 (campos y repositorio) | Hechas. Los tests de repositorio con MongoDB real quedan `SKIPPED` sin Docker: correrlos con Docker encendido |
| 09, 10, 11 y 15 (orquestador, manejadores, intentos, job y plantilla de extensión) | Hechas, con pruebas unitarias. La prueba de integración con MongoDB real de la UT 09 queda para la UT 12 |
| 12 (punta a punta) | Pendiente: necesita Docker y `PAGOS_CREDENCIAL_ACADEMICO` en el `.env` |
| 13 (documentación) | Hecha |
| 16 (flujo en vivo de CU14 usa el orquestador) | **No se hizo**, a propósito: es opcional, cambia el constructor de `ReservaService` y por lo tanto sus tests de CU14. Queda como follow-up; el flujo en vivo sigue con su propia copia de la lógica y funciona igual |
| 17 y 18 | Esperan a CU15 |

## Follow-ups (no se implementan en estas UT)

- **UT 17 y 18** (cuando exista CU15): `ManejadorDeCancelacion` y `ManejadorDeReprogramacion`, cada uno con sus estados y su prueba. No editan el núcleo del orquestador.
- **Ver las reservas en revisión** (listado o aviso al Administrador): hoy solo en el log y en el campo `requiereRevision`.
- **Bloqueo distribuido del job** si Académico pasa a más de una réplica.

## Qué NO incluyen estas UT

- Cancelar o reprogramar una clase (CU15) ni sus estados, endpoints o pantallas.
- Cambios en la app móvil o en la web: el job no tiene pantalla.
- Eventos de RabbitMQ ni la tabla `SagaEstado`: no se usan.
- Registrar los endpoints internos en el gateway: **está prohibido**.
- Modificar `ReservaService` o `PagosClient` **antes** de la UT 16, y solo ella lo hace.
