# Unidades de trabajo — CU17-BE

**Caso de uso:** Gestionar clases (asistencia y cierre): el instructor cierra una clase que ya empezó (asistencia, tema, observaciones y kilómetros) y puede corregir esos datos después; la reserva queda `COMPLETADA` o `NO_ASISTIO`.
**Microservicio(s):** `academico` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Pagos, Identidad y el gateway **no cambian**: cerrar no llama a Pagos (la hora ya se descontó y nunca se devuelve), Identidad ya da los nombres de estudiantes a un instructor (`POST /api/v1/estudiantes/consulta`) y el recurso `clases` ya está en el gateway y en `MICROSERVICIOS.md`.
**Fuente CU:** [`CU17 - Gestionar clases (asistencia y cierre)`](../../../CasosDeUso/CU17%20-%20Gestionar%20clases%20%28asistencia%20y%20cierre%29.md)
**Recurso HTTP / contrato:** `POST /api/v1/clases`, `GET /api/v1/clases/{codigo}` y `PATCH /api/v1/clases/{codigo}` (Académico, solo rol `INSTRUCTOR`), y dos campos nuevos en la agenda de [CU16](../../CU16%20-%20Consultar%20horario%20de%20clases/UT-BACKEND/README.md)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `academico`; un módulo habla con otro por el service del dueño), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query; todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`PATCH` para editar), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca loguear el texto de las observaciones), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y lo construido por [CU16-BE](../../CU16%20-%20Consultar%20horario%20de%20clases/UT-BACKEND/README.md) (`AgendaService`, `ClaseAgendaResponse`, `iniciarCancelacionPorInstructor`) y [CU15-BE](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-BACKEND/README.md) (`PlazoDeClase`). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Recurso `clases`, no una acción sobre `reservas`.** Cerrar crea la clase dictada (`Clase`, con modelo ya existente), así que es `POST /api/v1/clases` con el código de la reserva en el cuerpo; verla y corregirla son `GET` y `PATCH /api/v1/clases/{codigo}`. El recurso `clases` ya está en el gateway y en `MICROSERVICIOS.md`: **no se toca ninguno**. Un `ClaseService` y un `ClaseController` nuevos (`features/clases/`), sin engordar `ReservaService` ni `AgendaService`.
2. **El código de la URL es el `codigoClase`**, no el de la reserva. La agenda de CU16 lo entrega (`claseCodigo`) en las clases ya cerradas.
3. **Orden del cierre: guardar la clase y después marcar la reserva, con compensación.** Mongo no tiene transacción entre dos documentos aquí, así que cada paso se diseña repetible:
   1. Se valida la reserva (`CONFIRMADA`, del instructor, ya empezó).
   2. Se guarda la `Clase` (`CERRADA`). El índice único de `reservaId` impide dos clases por reserva.
   3. Se marca la reserva con un `findAndModify` atómico `CONFIRMADA → COMPLETADA | NO_ASISTIO`.
   4. Si el paso 3 devuelve vacío (otra petición cambió la reserva, por ejemplo el estudiante canceló), y la clase la creó **esta** petición, se **borra físicamente** (es un dato derivado y el índice único seguiría ocupando la reserva si solo se marcara) y se responde `409 ERR_ACA_37`.
   5. Si el servicio se cae entre el 2 y el 3, queda una clase con la reserva todavía `CONFIRMADA`: cerrar de nuevo **reutiliza** esa clase (la actualiza con lo nuevo) y termina el paso 3. No hay job: es el instructor quien repite (S1 del CU).
4. **Un módulo habla con el otro por su service.** `ClaseService` (módulo `clases`) pide la reserva y la marca por un `CierreDeReservaService` nuevo del módulo `reservas` (nunca por `ReservaRepository`). Y la agenda (módulo `reservas`) pide los códigos de clase por `ClaseService.codigosPorReserva`. Es una dependencia en los dos sentidos entre los módulos, **aceptada** porque la relación de negocio lo justifica (una clase dictada nace de una reserva y la agenda muestra ambas); entre beans no hay ciclo (`CierreDeReservaService` no conoce a `ClaseService`).
5. **Qué se puede cerrar:** la reserva está `CONFIRMADA` (no `REPROGRAMACION_PENDIENTE` ni transitoria) y `PlazoDeClase.haEmpezado` es verdadero. No espera a la hora de fin. Si no existe, está borrada o es de otro instructor: `404 ERR_ACA_35` (se amplía su descripción, hoy habla solo de cancelar). Si no se puede cerrar: `409 ERR_ACA_37`.
6. **Asistencia y estado:** `PRESENTE` y `TARDE` → reserva `COMPLETADA`; `AUSENTE` → `NO_ASISTIO`. La transición es única y atómica. No se llama a Pagos, la franja sigue `RESERVADA` y el vehículo queda vacío.
7. **Reglas de los datos** (`ReglasDeClase`, una sola clase compartida por cerrar y corregir):
   - `tema` hasta 120 y `observaciones` hasta 500 (lo comprueba Bean Validation: `400 ERR_SYS_01`).
   - `kmInicio` y `kmFin` enteros de 0 a 9 999 999, cada uno opcional (también Bean Validation).
   - Reglas de negocio con `ValidationException` `ERR_ACA_38` (`400`): si hay los dos kilometrajes, `kmFin >= kmInicio` (en la corrección se mira el resultado ya combinado con lo guardado), y con `AUSENTE` solo se aceptan observaciones (tema y km traen `400`).
   - Un texto en blanco se guarda como vacío (`null`).
8. **`PATCH` parcial:** lo que no se envía no se toca; un texto en blanco **borra** ese texto; un kilometraje no se puede borrar (solo cambiar). La asistencia **no** se puede enviar. Con una clase `AUSENTE` solo se acepta `observaciones`. Se edita siempre que la clase exista y sea del instructor, sin plazo.
9. **Agenda de CU16, dos campos nuevos** (cambio compatible hacia atrás; `UT-CU17-BE-08`): `cerrable` (`CONFIRMADA` que ya empezó, lo calcula el servidor) y `claseCodigo` (el `codigoClase`, solo en reservas `COMPLETADA` o `NO_ASISTIO` que ya tienen clase; si no, `null`). `cancelable` y `cerrable` nunca son verdaderos a la vez.
10. **Nombre del estudiante** en las respuestas de la clase con `IdentidadClient.nombresDeEstudiantes` (reenvía el Bearer del instructor). Si Identidad falla o no lo devuelve, la respuesta lleva `Estudiante` **sin error**: el cierre o la corrección ya están hechos (como cancelar en CU16).
11. **Errores nuevos (siguiente libre `ERR_ACA_37`):** `ERR_ACA_37` (409, la clase no se puede cerrar) y `ERR_ACA_38` (400, los datos de la clase no son válidos). `ERR_ACA_35` se reutiliza para «no existe o es de otro instructor» en las tres rutas. Se reutilizan también `ERR_ACA_16` y `ERR_SYS_01`. Cada código se documenta en `docs/errors/academico/` en la UT que lo introduce.
12. **Lo que el diseño de `clases` deja sin uso, a propósito:** `EstadoClase.ABIERTA`, `inicioReal`, `finReal` y `vehiculoCodigo`. La clase siempre se guarda `CERRADA`. Se anota en `DISENO_BASE_DE_DATOS.md` (UT 10); no hay migración ni cambio de índices.
13. **Cerrar y estudiante a la vez:** la transición de la reserva es condicional sobre `CONFIRMADA`, así que si el estudiante cancela o el instructor cierra dos veces a la vez gana uno y el otro recibe `409`.

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `INSTRUCTOR` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol, `403 ERR_ACA_16` si el token no trae escuela). Respuestas con el sobre estándar. El instructor y su escuela salen del token.

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /api/v1/clases` | `CerrarClaseRequest` | `201`, `data`: `ClaseResponse`, «Clase cerrada correctamente» |
| `GET /api/v1/clases/{codigo}` | — | `200`, `data`: `ClaseResponse`, «Clase obtenida correctamente» |
| `PATCH /api/v1/clases/{codigo}` | `ActualizarClaseRequest` | `200`, `data`: `ClaseResponse`, «Clase actualizada correctamente» |

`CerrarClaseRequest`:

| Campo | Tipo | Reglas |
|-------|------|--------|
| `reservaCodigo` | UUID | Obligatorio. La reserva de la agenda (su `codigo`) |
| `asistencia` | `PRESENTE`, `TARDE` o `AUSENTE` | Obligatorio |
| `tema` | texto | Opcional, hasta 120. No con `AUSENTE` |
| `observaciones` | texto | Opcional, hasta 500 |
| `kmInicio`, `kmFin` | entero | Opcionales, de 0 a 9 999 999; `kmFin >= kmInicio` si vienen los dos. No con `AUSENTE` |

`ActualizarClaseRequest`: los mismos `tema`, `observaciones`, `kmInicio` y `kmFin`, todos opcionales y sin `asistencia` ni `reservaCodigo`. Un cuerpo sin ningún campo no cambia nada y responde `200`.

`ClaseResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `codigo` | UUID | El `codigoClase` |
| `fecha` | fecha | `yyyy-MM-dd` |
| `estudianteCodigo` | UUID | Código público del estudiante |
| `estudianteNombre` | texto | Nombre completo, o `Estudiante` si Identidad no lo devuelve |
| `asistencia` | `PRESENTE`, `TARDE` o `AUSENTE` | La registrada |
| `tema`, `observaciones` | texto o `null` | |
| `kmInicio`, `kmFin` | entero o `null` | |
| `estado` | `CERRADA` | Siempre (ver decisión 12) |

Cambios a `ClaseAgendaResponse` (CU16): `cerrable` (boolean) y `claseCodigo` (UUID o `null`).

### Errores (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_35` | 404 | `NotFoundException` | La reserva o la clase no existe, está borrada o es de otro instructor |
| `ERR_ACA_37` | 409 | `ConflictException` | La clase no se puede cerrar: no está `CONFIRMADA` (ya cerrada, cancelada, a medias...) o todavía no empezó; o cambió mientras se cerraba |
| `ERR_ACA_38` | 400 | `ValidationException` | `kmFin` menor que `kmInicio`, o tema o kilómetros en una clase `AUSENTE` |

Se reutilizan: `ERR_ACA_16` (el token no trae la escuela) y `ERR_SYS_01` (cuerpo, tamaños, rangos o `{codigo}` inválidos).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU17-BE-01](./UT-CU17-BE-01.md) | Reservas: la transición atómica de cierre | — |
| [UT-CU17-BE-02](./UT-CU17-BE-02.md) | `ClaseRepository` | — |
| [UT-CU17-BE-03](./UT-CU17-BE-03.md) | Errores `ERR_ACA_37` y `ERR_ACA_38` | — |
| [UT-CU17-BE-04](./UT-CU17-BE-04.md) | DTOs y reglas de los datos de la clase | 03 |
| [UT-CU17-BE-05](./UT-CU17-BE-05.md) | `CierreDeReservaService`: qué reserva se puede cerrar y marcarla | 01, 03 |
| [UT-CU17-BE-06](./UT-CU17-BE-06.md) | `ClaseService`: cerrar una clase | 02, 04, 05 |
| [UT-CU17-BE-07](./UT-CU17-BE-07.md) | `ClaseService`: ver y corregir una clase | 02, 04, 06 |
| [UT-CU17-BE-08](./UT-CU17-BE-08.md) | La agenda de CU16 con `cerrable` y `claseCodigo` | 02, 06 |
| [UT-CU17-BE-09](./UT-CU17-BE-09.md) | `ClaseController` | 06, 07 |
| [UT-CU17-BE-10](./UT-CU17-BE-10.md) | Prueba de punta a punta y documentación | 08, 09 |

**Cadena:** `01`, `02` y `03` son independientes → `04` (03) y `05` (01, 03) → `06` (02, 04, 05) → `07` (06) y `08` (06) → `09` (06, 07) → `10` (08, 09).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 4 y 5 | Guardar la clase y dejar la reserva `COMPLETADA` o `NO_ASISTIO` | 01, 02, 05, 06, 09 |
| F1 | Ver y corregir una clase cerrada | 02, 07, 09 |
| S1 | Terminar un cierre interrumpido sin duplicar la clase | 02, 06 |
| A1, A8 | Clase no cerrable, o que cambió mientras se cerraba | 03, 05, 06 |
| A2 | Clase inexistente o ajena | 05, 07 |
| A3 | Datos inválidos | 03, 04, 06, 07 |
| A4 | Identidad no responde | 06, 07 |
| A5, A6 | Rol, sesión, escuela y límite de peticiones | 09 |
| CA13 | La agenda ofrece cerrar y editar | 08 |
| CA01 a CA12, CA15 | Criterios de aceptación (CA14 se comprueba con el historial de CU14) | 01 a 10 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 09 | **Hechas** (2026-11-01), con pruebas: `academico` 648 tests unitarios sin fallos y `./mvnw checkstyle:check` limpio. Los tests de repositorio se corrieron contra el Mongo del compose (`ClaseRepositoryTest` 6, `ReservaRepositoryTest` 48), sin fallos |
| 10 (punta a punta) | **Pendiente**: necesita el stack reconstruido con este código y el JWT de un instructor de `escuelaexample` |

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **`Clase` ganó un constructor** (`reservaId`, `estudianteCodigo`, `instructorCodigo`, `fecha`, `asistencia`) que la deja `CERRADA`: el modelo no tenía cómo crearse con datos ni setters para esos campos, y la UT 02 decía que no cambiaba. Es el único cambio al modelo; no cambió ningún campo ni índice.
- **`ReglasDeClaseLimites`** (`features/clases/dto/`) guarda las tres constantes (120, 500 y 9 999 999): las anotaciones de Bean Validation las necesitan como constantes de compilación y así los dos requests no repiten el número.
- **`ClaseService` no usa `Clock`:** el reloj lo usa `CierreDeReservaService` (que decide si la clase empezó y pone el instante del cierre), así que la ficha de la UT 06 sobraba en ese punto.
- **`AgendaService` solo pide los códigos de clase de las reservas ya `COMPLETADA` o `NO_ASISTIO`** de la página (no «los `id` de todas», como decía la UT 08): una confirmada o cancelada nunca muestra `claseCodigo`, así que consultarla sería trabajo inútil. Además, al armar la respuesta se vuelve a comprobar el estado, para que una clase a medias de una reserva `CONFIRMADA` (S1) no aparezca.
- **Los tests de logs** (`ClaseServiceTest`) usan el `ListAppender` de Logback para comprobar que nunca se registra el texto del tema ni de las observaciones, ni el valor de un kilometraje; solo los nombres de los campos cambiados.
- **Los tests de repositorio con MongoDB real se omiten solos en esta máquina**; se corrieron parchando solo durante la corrida `MongoIntegrationTestBase` (base temporal `academico_ut_tmp`, ya borrada, archivo restaurado), como en CU15 y CU16.
- **El código de error de «no existe»** (`ERR_ACA_35`) ahora también cubre la reserva o la clase dictada de CU17; su sección en `docs/errors/` se amplió.

## Follow-ups (no se implementan en estas UT)

- **Cerrar automáticamente** las clases que el instructor nunca cerró (por ejemplo `NO_ASISTIO` pasado un tiempo).
- **Cambiar la asistencia** o reabrir una clase cerrada (la escuela).
- **Asociar el auto** a la clase (CU10 y CU11).
- **Mostrar al estudiante** el tema o las observaciones.
- **Avisar** al estudiante: CU37 y CU38.

## Qué NO incluyen estas UT

- La prueba de clase (CU18) y su nota.
- «Iniciar clase» como paso aparte, `ABIERTA`, hora real de inicio y fin.
- Cambios en Pagos, Identidad o el gateway.
- La app móvil: está en [`UT-MOBILE`](../UT-MOBILE/README.md).
- Eventos de RabbitMQ y notificaciones: no se publica nada.
