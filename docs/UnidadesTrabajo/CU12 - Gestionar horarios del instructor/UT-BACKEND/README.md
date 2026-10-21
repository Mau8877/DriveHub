# Unidades de trabajo — CU12-BE

**Caso de uso:** Gestionar horarios del instructor: crear y desactivar horarios semanales (plantillas), generar y mantener sus franjas, y consultarlas, bloquearlas y desbloquearlas.
**Microservicio(s):** `academico` (todas las UT) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). El gateway ya enruta `plantillas-horario` y `franjas` hacia Académico; no necesita cambios.
**Fuente CU:** [`CU12 - Gestionar horarios del instructor`](../../../CasosDeUso/CU12%20-%20Gestionar%20horarios%20del%20instructor.md)
**Recurso HTTP / contrato:** `/api/v1/plantillas-horario` y `/api/v1/franjas` (Académico, solo rol `INSTRUCTOR`)

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

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `INSTRUCTOR` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol). Respuestas con el sobre estándar.

### Horarios (`plantillas-horario`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/plantillas-horario` | `page`, `pageSize`, `filter[activa]` (`true`/`false`) | `200`, `data`: lista de `PlantillaHorarioResponse` + `meta`; orden por `diaSemana`, `horaInicio` |
| `POST /api/v1/plantillas-horario` | `CrearPlantillaHorarioRequest` | `201`, `data`: `PlantillaHorarioCreadaResponse` |
| `POST /api/v1/plantillas-horario/{codigo}/desactivar` | — | `200`, `data: null`, mensaje "Horario desactivado correctamente" |

`CrearPlantillaHorarioRequest`:

| Campo | Tipo | Regla |
|-------|------|-------|
| `diaSemana` | int | obligatorio, 1 a 7 (`ERR_SYS_01` si no) |
| `horaInicio`, `horaFin` | string `HH:mm` | obligatorios, 24 horas (`ERR_SYS_01` si no) |
| `duracionClaseMin` | int | obligatorio; 15 a 240 se valida en el servicio (`ERR_ACA_02`) |
| `vigenteDesde` | date | opcional, hoy por defecto |
| `vigenteHasta` | date | opcional |

`PlantillaHorarioResponse`: `codigo`, `diaSemana`, `horaInicio`, `horaFin`, `duracionClaseMin`, `vigenteDesde`, `vigenteHasta` (puede ser nulo), `activa`. `PlantillaHorarioCreadaResponse` = lo mismo + `franjasGeneradas` (int).

### Franjas (`franjas`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/franjas` | `page`, `pageSize`, `filter[fecha]` (`yyyy-MM-dd`), `filter[estado]` (`LIBRE`/`RESERVADA`/`BLOQUEADA`) | `200`, `data`: lista de `FranjaResponse` + `meta`; orden por `fecha`, `horaInicio` |
| `POST /api/v1/franjas/{codigo}/bloquear` | — | `200`, `data`: `FranjaResponse` (estado `BLOQUEADA`) |
| `POST /api/v1/franjas/{codigo}/desbloquear` | — | `200`, `data`: `FranjaResponse` (estado `LIBRE`) |

`FranjaResponse`: `codigo`, `fecha`, `horaInicio`, `horaFin`, `estado`. No expone ids internos ni el vehículo.

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
| `ERR_ACA_10` | 409 | `ConflictException` | La franja ya empezó o ya pasó |

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

**Cadena:** `02 → 05`; `05 + 03 + 04 + 06 → 07 → 08`; `05 + 03 + 04 + 06 → 09`; `05 + 07 → 10`; `01 + 07 + 08 + 09 → 11 → 12` (la 12 también necesita la 10). Las UT 01, 02, 03, 04 y 06 no dependen de ninguna y se pueden hacer en paralelo.

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
| A13, CA15 | Solo Instructor (401 y 403) | 01, 07 a 12 |
| A14, CA18 | Rate limit | ya cubierto por el gateway; sin UT nueva |

A15 (sin conexión) es de mobile.

## Follow-ups (no se implementan en estas UT)

- **Editar un horario** (cambiar horas o vigencia regenerando franjas) y **reactivar** uno desactivado.
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
