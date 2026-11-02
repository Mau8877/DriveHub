# Unidades de trabajo — CU16-BE

**Caso de uso:** Consultar horario de clases: el instructor ve su agenda de clases por día (con el nombre de cada estudiante) y cancela una clase que no puede dar; al estudiante se le devuelve siempre la hora y la franja queda `BLOQUEADA`.
**Microservicio(s):** `academico` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Pagos, Identidad y el gateway **no cambian**: Pagos ya devuelve horas (ruta interna de CU44 con la credencial de servicio), Identidad ya consulta nombres de estudiantes para un instructor (`POST /api/v1/estudiantes/consulta`, CU22) y el gateway ya enruta `/api/v1/reservas/**`.
**Fuente CU:** [`CU16 - Consultar horario de clases`](../../../CasosDeUso/CU16%20-%20Consultar%20horario%20de%20clases.md)
**Recurso HTTP / contrato:** `GET /api/v1/reservas/agenda` y `POST /api/v1/reservas/{codigo}/cancelar-por-instructor` (Académico, solo rol `INSTRUCTOR`)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `academico`), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query; todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (listados paginados, acciones como sub-recurso verbo), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y lo construido por [CU15-BE](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-BACKEND/README.md) (`ManejadorDeCancelacion`, `CompensadorDeReserva`, `PlazoDeClase`, `iniciarCancelacion`) y la guía de [CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Dos rutas nuevas bajo `reservas`, no un recurso nuevo.** La agenda es una vista de las reservas del instructor y cancelar actúa sobre una reserva, así que viven en `/api/v1/reservas/...` y **no hace falta tocar el gateway ni `MICROSERVICIOS.md`** (el recurso ya está). Para no mezclar roles en una misma ruta (`@PreAuthorize` va por método del service), la cancelación del instructor tiene su propia ruta (`cancelar-por-instructor`) y no reutiliza `/cancelar` del estudiante. Un `AgendaService` nuevo (`features/reservas/service/`) y un `AgendaController` nuevo (`features/reservas/controller/`) las atienden, sin engordar `ReservaService`.
2. **El identificador de la clase en la URL es el `codigo` de la reserva** (la clase dictada de CU17 todavía no existe y se crea después a partir de la reserva). El DTO lo llama `codigo`, como `ReservaResponse`.
3. **Qué clases devuelve la agenda** (reglas del CU): reservas del instructor en la fecha pedida en estado `CONFIRMADA`, `REPROGRAMACION_PENDIENTE`, `COMPLETADA`, `NO_ASISTIO`, `CANCELACION_PENDIENTE` o `CANCELADA` que **alguna vez estuvo confirmada** (una `PENDIENTE_PAGO` vencida, cancelada por el sistema, nunca fue una clase; es la misma técnica que ya usa `historial`: `historialEstados.estado = CONFIRMADA`). Se ordenan por `horaInicio`. El cliente nunca ve los estados transitorios: `CANCELACION_PENDIENTE` se muestra `CANCELADA` y `REPROGRAMACION_PENDIENTE`, `CONFIRMADA` (igual que en CU15).
4. **Fecha de la agenda:** `filter[fecha]=yyyy-MM-dd`, opcional; sin ella, hoy en la zona horaria de la escuela (el `Clock` inyectado). Un valor que no es fecha es `400 ERR_SYS_01`. Es un listado, así que sigue el contrato de `page`/`pageSize` (por defecto 20, máximo 100).
5. **Nombres de estudiantes** con `IdentidadClient.nombresDeEstudiantes` (ya existe, reenvía el Bearer del instructor y Identidad limita a su escuela). Al **ver la agenda**, si Identidad falla es `503 ERR_ACA_17`. Un estudiante que Identidad no devuelve se muestra con `Estudiante` (constante, como `Instructor` en CU14). Al **cancelar**, la cancelación ya está registrada, así que si Identidad falla la respuesta lleva el nombre `Estudiante` en vez de fallar (misma decisión que reprogramar en CU15).
6. **`cancelable` lo calcula el servidor:** `true` si la clase está `CONFIRMADA` y todavía no empezó (`PlazoDeClase.haEmpezado`). La app no repite la regla; sin plazo de 12 horas para el instructor.
7. **La cancelación del instructor reutiliza el camino de CU15** con tres diferencias, todas guardadas en la reserva para que el proceso programado de CU44 termine igual una cancelación interrumpida:
   - `iniciarCancelacionPorInstructor` (repositorio): la misma transición atómica `CONFIRMADA → CANCELACION_PENDIENTE`, pero filtra por el **instructor** (no por el estudiante), fija `sinDevolucion = false` y marca `canceladaPorInstructor = true`.
   - **Campo nuevo `canceladaPorInstructor`** (`boolean`, por defecto `false`) en `reservas`. Es lo que le dice al `ManejadorDeCancelacion` que el motivo es «Cancelada por el instructor» y que la franja se **bloquea** en vez de liberarse.
   - `FranjaRepository.bloquearReservada(codigoFranja, reservaId)`: pasa la franja de `RESERVADA` (de esa reserva) a `BLOQUEADA` y quita el `reservaId`; repetirlo no cambia nada.
8. **`CompensadorDeReserva` se generaliza por segunda vez:** gana un parámetro `bloquearFranja` (por defecto `false`, así lo de CU14, CU15 y CU44 no cambia). Es el único archivo de CU15 que estas UT editan, junto con `ManejadorDeCancelacion` (que lee la marca de la reserva).
9. **Pagos con la credencial de servicio.** El instructor no puede devolverle horas a un estudiante con su propio Bearer (la billetera es del estudiante), así que en vivo se usa el cliente de servicio (`@Qualifier("clienteDeServicio")`, el mismo que usa el job), con `estudianteCodigo` de la reserva. Si Pagos no responde, la cancelación **se acepta** y el job devuelve la hora (`EN_PROCESO`). La respuesta no trae saldo: la billetera es del estudiante, no del instructor.
10. **Errores nuevos (siguiente libre `ERR_ACA_35`):** `ERR_ACA_35` (404, la clase no existe o no es del instructor) y `ERR_ACA_36` (409, la clase no se puede cancelar: no está `CONFIRMADA` o ya empezó). Se separan de `ERR_ACA_32`/`33` porque sus mensajes y su significado hablan del estudiante. Se reutilizan `ERR_ACA_16`, `ERR_ACA_17` y `ERR_SYS_01`. Cada código se documenta en `docs/errors/academico/` en la UT que lo introduce.
11. **Cancelar y estudiante a la vez:** las dos transiciones son un `findAndModify` condicional sobre `CONFIRMADA`, así que si el estudiante y el instructor cancelan al mismo tiempo gana uno y el otro recibe `409`.

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `INSTRUCTOR` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol, `403 ERR_ACA_16` si el token no trae escuela). Respuestas con el sobre estándar. El instructor y su escuela salen del token.

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/reservas/agenda` | `filter[fecha]` (opcional), `page`, `pageSize` | `200`, `data`: lista de `ClaseAgendaResponse` + `meta`, «Agenda obtenida correctamente» |
| `POST /api/v1/reservas/{codigo}/cancelar-por-instructor` | — (sin cuerpo) | `200`, `data`: `CancelacionInstructorResponse`, «Clase cancelada correctamente» |

`ClaseAgendaResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `codigo` | UUID | Código de la reserva |
| `fecha` | fecha | `yyyy-MM-dd` |
| `horaInicio`, `horaFin` | texto | `HH:mm` |
| `estudianteCodigo` | UUID | Código público del estudiante |
| `estudianteNombre` | texto | Nombre completo, o `Estudiante` si Identidad no lo devuelve |
| `estado` | `CONFIRMADA`, `COMPLETADA`, `NO_ASISTIO` o `CANCELADA` | Estado visible (sin transitorios) |
| `terminada` | boolean | La hora de fin ya pasó; una `CONFIRMADA` terminada es «pendiente de cierre» (CU17) |
| `cancelable` | boolean | `CONFIRMADA` y todavía no empezó |

`CancelacionInstructorResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `clase` | `ClaseAgendaResponse` | La clase ya `CANCELADA` |
| `horaDevuelta` | `DEVUELTA` o `EN_PROCESO` | `DEVUELTA`: la hora volvió a la billetera del estudiante. `EN_PROCESO`: Pagos no respondió, el sistema la devuelve solo. Nunca `PERDIDA` |

### Errores (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_35` | 404 | `NotFoundException` | La clase no existe, está borrada o es de otro instructor |
| `ERR_ACA_36` | 409 | `ConflictException` | La clase no está `CONFIRMADA` (ya cancelada, cerrada, a medias...) o ya empezó |

Se reutilizan: `ERR_ACA_16` (el token no trae la escuela), `ERR_ACA_17` (Identidad no responde al ver la agenda) y `ERR_SYS_01` (fecha o `{codigo}` inválidos).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU16-BE-01](./UT-CU16-BE-01.md) | Reserva: marca de cancelación por el instructor y su transición atómica | — |
| [UT-CU16-BE-02](./UT-CU16-BE-02.md) | Repositorio de reservas: la agenda del instructor | — |
| [UT-CU16-BE-03](./UT-CU16-BE-03.md) | Repositorio de franjas: bloquear una franja reservada | — |
| [UT-CU16-BE-04](./UT-CU16-BE-04.md) | Terminar la cancelación del instructor (`CompensadorDeReserva` y `ManejadorDeCancelacion`) | 01, 03 |
| [UT-CU16-BE-05](./UT-CU16-BE-05.md) | Errores `ERR_ACA_35` y `ERR_ACA_36` | — |
| [UT-CU16-BE-06](./UT-CU16-BE-06.md) | `AgendaService`: ver la agenda | 02, 05 |
| [UT-CU16-BE-07](./UT-CU16-BE-07.md) | `AgendaService`: cancelar por el instructor | 01, 04, 05, 06 |
| [UT-CU16-BE-08](./UT-CU16-BE-08.md) | Endpoints de la agenda y de cancelar por el instructor | 06, 07 |
| [UT-CU16-BE-09](./UT-CU16-BE-09.md) | Prueba de punta a punta y documentación | 08 |

**Cadena:** `01`, `02`, `03` y `05` son independientes → `04` (01, 03) y `06` (02, 05) → `07` (01, 04, 05, 06) → `08` (06, 07) → `09`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Ver la agenda del día, ordenada, con estudiante y estado | 02, 06, 08 |
| F1, pasos 4 y 5 | Cancelar: clase `CANCELADA`, hora devuelta, franja `BLOQUEADA` | 01, 03, 04, 07, 08 |
| S1 | Terminar una cancelación interrumpida con la franja bloqueada | 01, 03, 04 |
| A1 | Día sin clases: lista vacía | 02, 06 |
| A2, A3 | Clase no cancelable, ajena o inexistente | 05, 07 |
| A4 | Pagos o Identidad no responden | 06, 07 |
| A5, A6 | Rol, sesión, escuela y límite de peticiones | 08 |
| CA01 a CA11, CA13 | Criterios de aceptación (CA12 es de la app) | 02 a 09 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 08 | **Hechas** (2026-11-01), con pruebas: `academico` 579 tests unitarios sin fallos (`AgendaServiceTest` 23, `AgendaControllerTest` 13) y `./mvnw checkstyle:check` limpio. Los tests de repositorio se corrieron contra el Mongo del compose (`ReservaRepositoryTest` 43, `FranjaRepositoryTest` 43), sin fallos |
| 09 (punta a punta) | **Pendiente**: necesita el stack reconstruido con este código y el JWT de un instructor de `escuelaexample` |

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **`AgendaService.agenda` y `cancelar` reciben también `escuelaCodigo`** (del token, vía `@CurrentEscuelaId`) y lanzan `403 ERR_ACA_16` si es nulo, igual que `reservar` en CU14. Las firmas son `agenda(instructorCodigo, escuelaCodigo, bearer, fecha, page, pageSize)` y `cancelar(instructorCodigo, escuelaCodigo, bearer, codigoReserva)`.
- **`page` y `pageSize` se acotan, no se rechazan** (como `ReservaService.listar`): `page` menor que 1 es 1 y `pageSize` va de 1 a 100. Solo una fecha inválida es `400 ERR_SYS_01`.
- **`terminada` reutiliza `ReservaService.terminada`** (método estático del mismo paquete); no hubo que extraerla a `PlazoDeClase`.
- **`ManejadorDeCancelacion` sigue llamando a la sobrecarga de cinco parámetros del compensador para el estudiante** (sus tests no cambiaron) y usa la nueva, con `bloquearFranja`, solo para `canceladaPorInstructor`. `ManejadorDeCancelacion.MOTIVO_INSTRUCTOR` es la constante del motivo.
- **El nombre del estudiante usa `IdentidadClient.nombresDeEstudiantes`** tal cual existía (CU22); no hizo falta tocar Identidad.
- **Los tests de repositorio con MongoDB real se omiten solos en esta máquina**; se corrieron parchando solo durante la corrida `MongoIntegrationTestBase` (base temporal `academico_ut_tmp`, ya borrada, archivo restaurado), como en CU15.
- **`ClaseAgendaResponse` ganó `cerrable` y `claseCodigo` con el CU17** ([UT-CU17-BE-08](../../CU17%20-%20Gestionar%20clases%20%28asistencia%20y%20cierre%29/UT-BACKEND/UT-CU17-BE-08.md)): cambio compatible hacia atrás. `AgendaService` recibe ahora también `ClaseService`.

## Follow-ups (no se implementan en estas UT)

- **Avisar** al estudiante de la cancelación: CU37 y CU38.
- **Cerrar la clase** (asistencia, notas): CU17, que dará uso al estado «Pendiente de cierre» que la agenda ya muestra.
- **Cancelación por el Administrador** (web): si se necesita, reutilizará `iniciarCancelacionPorInstructor` con otra regla de pertenencia.

## Qué NO incluyen estas UT

- Reprogramar una clase por el instructor, cerrar la clase y la asistencia (CU17), el calendario del estudiante.
- Cambios en Pagos, Identidad o el gateway.
- La app móvil: está en [`UT-MOBILE`](../UT-MOBILE/README.md).
- Eventos de RabbitMQ y notificaciones: no se publica nada.
- Reembolsos de dinero (CU28 y CU31).
