# Unidades de trabajo — CU12-BE

**Caso de uso:** Gestionar horarios del instructor: crear y desactivar horarios semanales (plantillas), generar y mantener sus franjas, y consultarlas, bloquearlas y desbloquearlas.
**Microservicio(s):** `academico` (todas las UT) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). El gateway ya enruta `plantillas-horario` y `franjas` hacia Académico; no necesita cambios.
**Fuente CU:** [`CU12 - Gestionar horarios del instructor`](../../../CasosDeUso/CU12%20-%20Gestionar%20horarios%20del%20instructor.md)
**Recurso HTTP / contrato:** `/api/v1/plantillas-horario` y `/api/v1/franjas` (Académico, solo rol `INSTRUCTOR`)

> **Refactor del CU12 (varios días, nombre, pausar y bloquear un día).** Las UT 01 a 12 ya se implementaron y quedan como historia del primer alcance. Las **UT 13 a 20** cambian ese código al contrato nuevo del CU. Donde una decisión o una tabla de abajo contradice una anterior, vale la que está marcada como vigente.

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Un horario no se edita.** Solo se crea y se desactiva. Editar obligaría a regenerar franjas y a decidir qué hacer con las ya reservadas; desactivar + crear logra lo mismo sin esa complejidad. La edición queda como follow-up.
2. **Las franjas no llevan vehículo todavía.** `vehiculoCodigo` queda vacío: la relación auto–instructor es CU10 (Escuela), que no existe. Problema real que esto destapa: el índice único `uq_franja_vehiculo_fecha_hora` hoy trata la ausencia de vehículo como un valor más, así que dos franjas sin vehículo en la misma fecha y hora chocarían aunque sean de instructores distintos. La UT 02 lo convierte en un índice **parcial** (solo cuando hay vehículo).
3. **`escuelaCodigo` en horarios y franjas.** Se guarda el claim `escuela` del JWT al crear. Es un campo, no el filtro por tenant (ese mecanismo sigue pendiente, ver [DISENO_BASE_DE_DATOS.md](../../../DriveHub/DISENO_BASE_DE_DATOS.md#10-diferencias-con-el-pdf-y-pendientes)); guardarlo desde el primer documento evita rellenarlo después consultando a Identidad por cada instructor.
4. **Sin transacciones.** `AcademicoApplication` no tiene `MongoTransactionManager`, así que un `@Transactional` no haría nada. Se diseña sin depender de eso: la generación es **idempotente** (el índice único `instructorCodigo + fecha + horaInicio` rechaza duplicados, que se ignoran) y la generación diaria completa lo que falte. Al desactivar, primero se eliminan las franjas y al final se marca el horario inactivo: si falla a medias, el horario sigue activo y se puede repetir.
5. **Franjas quitadas = borrado físico.** Son datos derivados del horario. Un borrado lógico mantendría ocupada la clave única `(instructor, fecha, hora)` y bloquearía crear un horario nuevo en la misma hora. Es la única excepción a "soft delete" en este CU.
6. **Generación: 28 días contando hoy** (`app.horarios.horizonte-dias=28`, así cada día de la semana aparece exactamente 4 veces) **y en la zona horaria de la escuela** (`app.horarios.zona-horaria`, por defecto `America/La_Paz`: **confirmar** que es la correcta). Se inyecta un `Clock` para probar el tiempo.
7. **Generación diaria con `@Scheduled`** (`app.horarios.extension-cron`, por defecto a las 02:00). Sirve para una sola instancia del servicio; con varias réplicas se necesitaría un candado (ShedLock). Aceptado a propósito, mismo criterio que el rate limit del gateway.
8. **Autorización en el servicio** con `@PreAuthorize("hasRole('INSTRUCTOR')")`; el instructor sale del `sub` del token (`@CurrentUserId`), nunca de un parámetro de la petición. Un horario o franja de otro instructor responde 404, no 403, para no revelar que existe.
9. **`filter[activa]` omitido = todos los horarios** (activos e inactivos); la app manda `filter[activa]=true` por defecto. `filter[fecha]` y `filter[estado]` son filtros exactos; sin `filter[fecha]`, `GET /franjas` devuelve las franjas desde hoy en adelante.
10. **Entrada por `GlobalExceptionHandler`:** Académico no maneja JSON malformado, parámetros con tipo incorrecto ni parámetros faltantes (caerían en el `500`). La UT 11 agrega esos handlers → `400 ERR_SYS_01`.
11. **Un horario guarda varios días (vigente desde el refactor).** `diasSemana` (lista, 1 a 7, sin repetir) y `nombre` reemplazan a `diaSemana`; la ventana de horas es la misma para todos los días. El `estado` (`ACTIVA`, `PAUSADA`, `INACTIVA`) reemplaza al booleano `activa`. Los documentos existentes se convierten al arrancar con una migración idempotente (UT 13), sin script manual.
12. **El listado de horarios ordena y pagina en memoria.** El CU pide los activos primero y después los pausados, pero el orden alfabético del estado (`ACTIVA`, `INACTIVA`, `PAUSADA`) no lo da. Un instructor tiene pocos horarios, así que se leen todos los del instructor, se ordenan en Java y se pagina el resultado; el contrato (`page`, `pageSize`, `meta`) no cambia. **Reemplaza a la decisión 9** en lo de `filter[activa]`: ahora es `filter[estado]` repetible, y sin él se devuelven activos y pausados.
13. **Pausar no es transaccional y reanudar vuelve a validar.** Pausar borra primero las franjas y marca el estado al final (como desactivar). Reanudar comprueba otra vez el cruce, porque en la pausa pudo crearse otro horario activo que choque, y marca `ACTIVA` antes de generar: si la generación falla, la tarea diaria completa lo que falte. Los bloqueos manuales de las franjas borradas no se conservan.
14. **Bloquear un día es un `updateMulti` atómico por documento.** Toca solo franjas del estado esperado y, si el día es hoy, solo las que aún no empezaron. Es repetible: ejecutarlo dos veces no cambia nada más. La consulta por rango admite hasta 7 días contando ambos extremos para no devolver listados enormes.

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `INSTRUCTOR` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol). Respuestas con el sobre estándar.

### Horarios (`plantillas-horario`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/plantillas-horario` | `page`, `pageSize`, `filter[estado]` (`ACTIVA`/`PAUSADA`/`INACTIVA`, repetible; sin él, activos y pausados) | `200`, `data`: lista de `PlantillaHorarioResponse` + `meta`; orden por estado (activos, pausados, inactivos), primer día y hora de inicio |
| `POST /api/v1/plantillas-horario` | `CrearPlantillaHorarioRequest` | `201`, `data`: `PlantillaHorarioCreadaResponse` |
| `POST /api/v1/plantillas-horario/{codigo}/pausar` | — | `200`, `data: null`, «Horario pausado correctamente» |
| `POST /api/v1/plantillas-horario/{codigo}/reanudar` | — | `200`, `data: null`, «Horario reanudado correctamente» |
| `POST /api/v1/plantillas-horario/{codigo}/desactivar` | — | `200`, `data: null`, «Horario desactivado correctamente» |

`CrearPlantillaHorarioRequest`:

| Campo | Tipo | Regla |
|-------|------|-------|
| `nombre` | string | obligatorio, hasta 60 caracteres (`ERR_SYS_01` si no) |
| `diasSemana` | lista de int | obligatoria, de 1 a 7 elementos, cada uno de 1 a 7; los repetidos se ignoran (`ERR_SYS_01` si no) |
| `horaInicio`, `horaFin` | string `HH:mm` | obligatorios, 24 horas (`ERR_SYS_01` si no) |
| `duracionClaseMin` | int | obligatorio; 15 a 240 se valida en el servicio (`ERR_ACA_02`) |
| `vigenteDesde` | date | opcional, hoy por defecto |
| `vigenteHasta` | date | opcional |

`PlantillaHorarioResponse`: `codigo`, `nombre`, `diasSemana`, `horaInicio`, `horaFin`, `duracionClaseMin`, `vigenteDesde`, `vigenteHasta` (puede ser nulo), `estado`. `PlantillaHorarioCreadaResponse` = lo mismo + `franjasGeneradas` (int).

### Franjas (`franjas`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/franjas` | `page`, `pageSize`, `filter[fecha]` (`yyyy-MM-dd`) **o** `filter[fechaDesde]` y `filter[fechaHasta]` (rango de hasta 7 días), `filter[estado]` (`LIBRE`/`RESERVADA`/`BLOQUEADA`) | `200`, `data`: lista de `FranjaResponse` + `meta`; orden por `fecha`, `horaInicio`; sin fechas, desde hoy |
| `POST /api/v1/franjas/{codigo}/bloquear` | — | `200`, `data`: `FranjaResponse` (estado `BLOQUEADA`) |
| `POST /api/v1/franjas/{codigo}/desbloquear` | — | `200`, `data`: `FranjaResponse` (estado `LIBRE`) |
| `POST /api/v1/franjas/dias/{fecha}/bloquear` | — | `200`, `data`: `DiaBloqueadoResponse` |
| `POST /api/v1/franjas/dias/{fecha}/desbloquear` | — | `200`, `data`: `DiaBloqueadoResponse` |

`FranjaResponse`: `codigo`, `fecha`, `horaInicio`, `horaFin`, `estado`. No expone ids internos ni el vehículo. `DiaBloqueadoResponse`: `fecha`, `franjasAfectadas` (cero si no había nada que cambiar).

### Errores nuevos (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_01` | 400 | `ValidationException` | `horaFin` no es posterior a `horaInicio` |
| `ERR_ACA_02` | 400 | `ValidationException` | Duración fuera de 15–240 o el tramo no es múltiplo exacto de ella |
| `ERR_ACA_03` | 400 | `ValidationException` | `vigenteDesde` anterior a hoy o `vigenteHasta` anterior a `vigenteDesde` |
| `ERR_ACA_04` | 409 | `ConflictException` | Se cruza con otro horario activo del instructor |
| `ERR_ACA_05` | 404 | `NotFoundException` | El horario no existe o no es del instructor |
| `ERR_ACA_06` | 409 | `ConflictException` | El horario ya está inactivo |
| `ERR_ACA_07` | 404 | `NotFoundException` | La franja no existe o no es del instructor |
| `ERR_ACA_08` | 409 | `ConflictException` | Bloquear una franja que no está `LIBRE` |
| `ERR_ACA_09` | 409 | `ConflictException` | Desbloquear una franja que no está `BLOQUEADA` |
| `ERR_ACA_10` | 409 | `ConflictException` | La franja ya empezó o ya pasó, o se pide bloquear o desbloquear un día que ya pasó |
| `ERR_ACA_11` | 409 | `ConflictException` | Pausar un horario que no está activo |
| `ERR_ACA_12` | 409 | `ConflictException` | Reanudar un horario que no está pausado |
| `ERR_ACA_13` | 400 | `ValidationException` | Rango de fechas con la inicial posterior a la final, de más de 7 días o con una sola fecha |

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU12-BE-01](./UT-CU12-BE-01.md) | `@CurrentUserId` y escuela del token | `academico` | — |
| [UT-CU12-BE-02](./UT-CU12-BE-02.md) | Models de horarios: escuela, índice de vehículo parcial y constructores | `academico` | — |
| [UT-CU12-BE-03](./UT-CU12-BE-03.md) | Catálogo `AcademicoErrorCodes` y su documentación | `academico` | — |
| [UT-CU12-BE-04](./UT-CU12-BE-04.md) | `GeneradorFranjas`, `Clock` y configuración de horarios | `academico` | — |
| [UT-CU12-BE-05](./UT-CU12-BE-05.md) | Repositorios de horarios y franjas | `academico` | 02 |
| [UT-CU12-BE-06](./UT-CU12-BE-06.md) | DTOs de horarios y franjas | `academico` | — |
| [UT-CU12-BE-07](./UT-CU12-BE-07.md) | `PlantillaHorarioService` (crear y listar) y `FranjaGeneracionService.generar` | `academico` | 02, 03, 04, 05, 06 |
| [UT-CU12-BE-08](./UT-CU12-BE-08.md) | `PlantillaHorarioService`: desactivar | `academico` | 03, 05, 07 |
| [UT-CU12-BE-09](./UT-CU12-BE-09.md) | `FranjaService`: listar, bloquear y desbloquear | `academico` | 03, 04, 05, 06 |
| [UT-CU12-BE-10](./UT-CU12-BE-10.md) | Generación diaria de franjas | `academico` | 05, 07 |
| [UT-CU12-BE-11](./UT-CU12-BE-11.md) | Controllers y manejo de entrada inválida | `academico` | 01, 07, 08, 09 |
| [UT-CU12-BE-12](./UT-CU12-BE-12.md) | Prueba de integración del CU12 | `academico` | 10, 11 |
| [UT-CU12-BE-13](./UT-CU12-BE-13.md) | Modelo del horario con nombre, varios días y estado | `academico` | — |
| [UT-CU12-BE-14](./UT-CU12-BE-14.md) | Errores ERR_ACA_11 a ERR_ACA_13 y su documentación | `academico` | — |
| [UT-CU12-BE-15](./UT-CU12-BE-15.md) | Generación de franjas para varios días | `academico` | 13 |
| [UT-CU12-BE-16](./UT-CU12-BE-16.md) | Repositorios: horarios por estado y franjas por rango y por día | `academico` | 13 |
| [UT-CU12-BE-17](./UT-CU12-BE-17.md) | DTOs y `PlantillaHorarioService`: crear y listar con varios días | `academico` | 13, 14, 15, 16 |
| [UT-CU12-BE-18](./UT-CU12-BE-18.md) | `PlantillaHorarioService`: pausar, reanudar y desactivar | `academico` | 14, 15, 16, 17 |
| [UT-CU12-BE-19](./UT-CU12-BE-19.md) | `FranjaService`: consulta por rango y bloqueo de un día | `academico` | 14, 16 |
| [UT-CU12-BE-20](./UT-CU12-BE-20.md) | Controllers, tarea diaria y prueba de integración | `academico` | 17, 18, 19 |

**Cadena:** `02 → 05`; `05 + 03 + 04 + 06 → 07 → 08`; `05 + 03 + 04 + 06 → 09`; `05 + 07 → 10`; `01 + 07 + 08 + 09 → 11 → 12` (la 12 también necesita la 10). Las UT 01, 02, 03, 04 y 06 no dependen de ninguna y se pueden hacer en paralelo.

**Cadena del refactor (UT 13 a 20):** `13 → 15, 16`; `13 + 14 + 15 + 16 → 17 → 18`; `14 + 16 → 19`; `17 + 18 + 19 → 20`. Las UT 13 y 14 no dependen de ninguna.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listar horarios, con y sin inactivos | 05, 06, 07, 11, 12 |
| F1, CA02, CA03, CA04 | Crear y generar franjas | 02, 04, 05, 07, 11, 12 |
| A1, CA05 | Formato inválido | 06, 11 |
| A2 a A4, CA06 | Hora, duración y vigencia inválidas | 03, 07 |
| A5, CA07 | Cruce de horarios | 03, 05, 07, 12 |
| F2, A12, CA08 | Listar franjas de una fecha | 05, 06, 09, 11, 12 |
| F3, F4, A9 a A11, CA09 a CA11 | Bloquear y desbloquear | 03, 05, 09, 11, 12 |
| A6, A7, A8, CA12, CA17 | Inexistente, ya inactivo, ajeno | 03, 05, 08, 09 |
| F5, CA13, CA14 | Desactivar | 05, 08, 12 |
| S1, A16, CA16 | Generación diaria idempotente | 04, 05, 07, 10 |
| A13, CA15 | Solo Instructor (401 y 403) | 01, 07 a 12, 17 a 20 |
| F1 con varios días, CA02, CA03, CA05, CA07 (refactor) | Nombre, varios días y cruce por día compartido | 13, 15, 17, 20 |
| F6, F7, A17, A18, CA19, CA20, CA21 | Pausar y reanudar | 13, 14, 15, 16, 18, 20 |
| F8, A11, CA22, CA23, CA24 | Bloquear y desbloquear un día | 14, 16, 19, 20 |
| F2 con rango, A19, CA25 | Consulta de la semana | 14, 16, 19, 20 |
| CA26 | Horarios anteriores al cambio | 13 |
| A14, CA18 | Rate limit | ya cubierto por el gateway; sin UT nueva |

A15 (sin conexión) es de mobile.

## Follow-ups (no se implementan en estas UT)

- **Editar un horario** (cambiar horas o vigencia regenerando franjas) y **reactivar** uno desactivado (pausar y reanudar ya cubren la pausa temporal).
- **Ventanas distintas por día dentro de un mismo horario** (hoy se crean varios horarios).
- **Vehículo en horarios y franjas**, cuando existan CU07 y CU10 (y con ello revisar el índice parcial).
- **Filtro automático por tenant** (`escuelaCodigo`) en Académico.
- **Validar que el instructor esté activo en Escuela** (CU09).
- **Marcar como vencidos** los horarios con la vigencia terminada (hoy siguen figurando `activa` aunque no generen nada).
- **Candado de la generación diaria** (ShedLock) si el servicio pasa a más de una réplica.
- Un `MongoTransactionManager` en Académico: lo necesitará CU14 (reserva + franja + outbox).

## Qué NO incluyen estas UT

- Reservar, cancelar o reprogramar una franja (CU14, CU15) ni pasarla a `RESERVADA`.
- Consultar la disponibilidad desde la escuela o el estudiante (CU11, CU13, CU16): `GET /franjas` es la vista del instructor sobre sus propias franjas.
- Autos, vehículos y asignaciones (CU07, CU10).
- Eventos de RabbitMQ ni outbox.
- Lógica en el controller ni logging en el controller.

## Commits sugeridos

Un solo commit `[BACK]`: `feat[BACK]: academico -- Gestionar horarios del instructor (CU12)`. Los cambios de documentación (catálogo de errores y diseño de base de datos) van en un commit `[DOCS]` aparte.
