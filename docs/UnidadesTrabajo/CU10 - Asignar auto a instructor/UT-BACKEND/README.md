# Unidades de trabajo — CU10-BE

**Caso de uso:** Asignar auto a instructor: listar los autos de la escuela con su instructor, asignar, cambiar de instructor, liberar y consultar el historial de un auto, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `escuela` (Clean) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU10 - Asignar auto a instructor.md`](../../../CasosDeUso/CU10%20-%20Asignar%20auto%20a%20instructor.md)
**Recurso HTTP / contrato:** `/api/v1/asignaciones-vehiculo` (ya enrutado por el gateway a `escuela` y listado en MICROSERVICIOS.md)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

**Requiere** CU07-BE y CU09-BE ya implementados: usa los autos (`VehiculoRepository`), los instructores (`InstructorRepository`) y el `InstructorCuentaGateway` (nombres desde Identidad).

## Decisiones de diseño

1. **El recurso es "la asignación de cada auto", con el código del auto como identificador.** El listado trae **un renglón por auto** (con o sin instructor), y las acciones cuelgan del auto: `POST /asignaciones-vehiculo/{vehiculoCodigo}/asignar`. No hay `POST` de creación genérico: así el listado, las acciones y el historial comparten la misma clave y el `<dh-gestion>` los usa sin adaptadores. La tabla y el gateway ya existen; no hay migración ni cambio de gateway.
2. **Sin migración.** `asignacion_vehiculo` y su índice único parcial `uq_asignacion_vehiculo_vigente` ya existen (migración de `V20261018120000`). Ese índice es la garantía real de "una sola vigente": si dos asignaciones simultáneas del mismo auto chocan, la segunda falla y se responde `ERR_ESC_16`, igual que la placa repetida en CU07.
3. **Cambiar de instructor es una transacción**: cerrar la vigente y abrir la nueva en el mismo `@Transactional` (A10 del CU). El repositorio expone una sola operación `cerrarYAbrir`, no dos llamadas sueltas del caso de uso.
4. **Tres casos de uso de escritura y dos de lectura**, uno por clase (`ejecutar(...)`), como pide la arquitectura clean. Las reglas comunes (auto `DISPONIBLE`, instructor `ACTIVO`, fechas) viven en una clase de reglas del dominio, como `ReglasVehiculo` en CU07.
5. **Los nombres vienen de Identidad.** Escuela solo guarda `usuarioCodigo` del instructor. El listado y el historial piden los nombres con `InstructorCuentaGateway.consultar(...)` (en lotes) y la búsqueda por nombre del instructor usa `buscarCodigos(...)`, igual que `ListarInstructoresUseCase`. Un nombre que Identidad no devuelve se muestra vacío, sin error.
6. **Fechas:** "hoy" sale del `Clock` ya configurado (`RelojConfig`). La fecha de inicio/fin no puede ser futura; el inicio de una asignación nueva tampoco puede ser anterior al fin de la última asignación cerrada del auto (no se solapan); el fin no puede ser anterior al inicio de la que se cierra. Las tres reglas responden `ERR_SYS_01`.
7. **Códigos de error nuevos** (siguen a `ERR_ESC_15`): `ERR_ESC_16` auto ya asignado (o mismo instructor), `ERR_ESC_17` auto no disponible, `ERR_ESC_18` instructor no activo, `ERR_ESC_19` auto sin asignación vigente. Se reusan `ERR_ESC_09`, `ERR_ESC_11` y `ERR_ESC_13`.
8. **El instructor se identifica con el `codigo` de su ficha** (`Instructor.codigo`), el mismo que expone `/instructores`, no con el `usuarioCodigo`.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/asignaciones-vehiculo?page&pageSize&search&filter[asignacion]` | 200 | `AsignacionVehiculoResponse[]` + `meta` |
| `POST` | `/api/v1/asignaciones-vehiculo/{vehiculoCodigo}/asignar` | 201 | `AsignacionVehiculoResponse` |
| `POST` | `/api/v1/asignaciones-vehiculo/{vehiculoCodigo}/cambiar-instructor` | 200 | `AsignacionVehiculoResponse` |
| `POST` | `/api/v1/asignaciones-vehiculo/{vehiculoCodigo}/liberar` | 200 | `AsignacionVehiculoResponse` |
| `GET` | `/api/v1/asignaciones-vehiculo/{vehiculoCodigo}/historial` | 200 | `HistorialAsignacionResponse[]` + `meta` |

`filter[asignacion]` acepta `ASIGNADO` o `SIN_ASIGNAR`. `search` busca en placa, marca, modelo y nombre del instructor.

Cuerpos: `asignar` y `cambiar-instructor` reciben `{ instructorCodigo, fecha }` (`fecha` opcional, hoy por defecto); `liberar` recibe `{ fecha }` opcional.

`AsignacionVehiculoResponse`: `vehiculoCodigo`, `placa`, `marca`, `modelo`, `estadoVehiculo`, `instructor` (`{ codigo, nombreCompleto }` o `null`) y `fechaInicio` (`null` si no hay instructor). Las respuestas de `asignar`, `cambiar-instructor` y `liberar` traen `nombreCompleto` en `null`: la pantalla recarga el listado, que sí lo trae.

`HistorialAsignacionResponse`: `codigo`, `instructor` (`{ codigo, nombreCompleto }`), `fechaInicio`, `fechaFin` (`null` = vigente).

> **El historial también se pagina:** [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) exige `meta` en todo listado, aunque un auto acumule pocas asignaciones. Acepta `page` y `pageSize`; la pantalla pide la primera página con `pageSize=100`.

Códigos de error nuevos: `ERR_ESC_16` a `ERR_ESC_19` ([docs/errors](../../../errors/escuela/ERRORES_ESCUELA.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU10-BE-01](./UT-CU10-BE-01.md) | Dominio y repositorio de asignaciones de autos | — |
| [UT-CU10-BE-02](./UT-CU10-BE-02.md) | Casos de uso: asignar, cambiar de instructor y liberar | 01 |
| [UT-CU10-BE-03](./UT-CU10-BE-03.md) | Casos de uso: listar y ver el historial | 01 |
| [UT-CU10-BE-04](./UT-CU10-BE-04.md) | Controller, errores y documentación | 02, 03 |

**Cadena:** `01 → 02, 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con búsqueda, filtro y paginación | 01, 03, 04 |
| F1 | Asignar | 01, 02, 04 |
| F2 | Cambiar de instructor | 01, 02, 04 |
| F3 | Liberar | 01, 02, 04 |
| F4 | Historial | 01, 03, 04 |
| A2 a A10 | 404, validación, auto no disponible, ya asignado, instructor no activo, sin vigente, fechas, atomicidad | 02, 04 |
| A11, A12 | Rol, token sin escuela, rate limit | 04 (rol y token); el rate limit es del gateway |

## Follow-ups (no se implementan en estas UT)

- Una asignación de ejemplo en los seeders de desarrollo (`VehiculoSeeder`, `InstructorSeeder`).
- Liberar solas las asignaciones cuando el auto pasa a mantenimiento o baja, o el instructor a licencia o inactivo.
- Exponer el auto del instructor en la app móvil y usarlo en horarios y clases (CU11, CU12, CU14).

## Qué NO incluyen estas UT

- Disponibilidad por fecha y hora (CU11), horarios, franjas y clases.
- Asignación de estudiantes (`asignacion_alumno`).
- Validar la categoría del auto contra las del instructor.
- Cambios en el gateway, en `MICROSERVICIOS.md` o en el esquema de la base: ya están.
- Frontend ([CU10-FE](../UT-FRONTEND/README.md)).
