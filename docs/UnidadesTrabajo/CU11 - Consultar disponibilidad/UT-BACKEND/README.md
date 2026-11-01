# Unidades de trabajo — CU11-BE

**Caso de uso:** Consultar disponibilidad: listar los instructores de la escuela para un día y un momento del día, con su disponibilidad, sus franjas libres, reservadas y bloqueadas, y el estado de sus autos, con búsqueda, filtros y paginación, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `escuela` (Clean), `academico` (capas) y `gateway` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU11 - Consultar disponibilidad.md`](../../../CasosDeUso/CU11%20-%20Consultar%20disponibilidad.md)
**Recurso HTTP / contrato:** `GET /api/v1/disponibilidad-instructores` (Escuela, nuevo en el gateway) y `GET /api/v1/franjas/resumen` (Académico, sobre el recurso `franjas` que el gateway ya enruta)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) y [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

**Requiere** CU09-BE, CU10-BE y CU12-BE ya implementados: usa los instructores (`InstructorRepository`), las asignaciones de autos (`AsignacionVehiculoRepository`), el `InstructorCuentaGateway` (nombres desde Identidad) y las franjas de Académico.

## Decisiones de diseño

1. **La consulta vive en Escuela**, el paquete P2 del CU. Escuela es quien conoce a los instructores (con su estado), sus autos y la relación entre ambos, y reutiliza la búsqueda por nombre de CU09. Académico solo aporta lo que es suyo: las franjas.
2. **Escuela pide a Académico un resumen por instructor**, no las franjas una por una: `GET /api/v1/franjas/resumen` devuelve, para un día y una ventana de horas, cuántas franjas tiene cada instructor de la escuela (`total`, `libres`, `reservadas`, `bloqueadas`). Reenvía el Bearer del Administrador (igual que CU23 y CU22) y Académico vuelve a validar el rol y toma la escuela del token. Un instructor sin franjas ese día **no viene** en el resumen.
3. **El instructor se cruza por su `usuarioCodigo`.** Las franjas guardan como `instructorCodigo` el `codigo_usuario` de Identidad; Escuela guarda ese mismo valor como `usuarioCodigo` de la ficha. Es la clave que une los dos servicios. El renglón de la respuesta se identifica con el `codigo` de la **ficha** (el mismo que expone `/instructores`), nunca con el `usuarioCodigo`.
4. **Paginación en memoria, sobre el resultado ya filtrado.** La disponibilidad se calcula con datos de Académico, así que no se puede filtrar ni ordenar en SQL. El caso de uso trae las fichas de la escuela (tope de 500, con un `WARN` si hay más), cruza sus autos y el resumen, filtra, ordena y recién entonces corta la página. Los nombres se piden a Identidad solo para las filas de la página (y la búsqueda por nombre usa `buscarCodigos`, igual que CU09).
5. **La disponibilidad se deriva, no se guarda.** `Disponibilidad` (`DISPONIBLE`, `COMPLETO`, `SIN_HORARIO`, `NO_DISPONIBLE`), `EstadoAutos` (`DISPONIBLE`, `NO_DISPONIBLE`, `SIN_AUTO`) y `MomentoDelDia` (`MANANA`, `TARDE`, `NOCHE`) son enums del dominio de Escuela con las reglas del CU. No hay migración ni cambio de esquema.
6. **«Hoy» sale del `Clock`** ya configurado (`RelojConfig`) en Escuela y del `Clock` del servicio en Académico. La fecha no puede ser anterior a hoy (`ERR_SYS_01`). Las franjas `LIBRE` que ya empezaron no cuentan como libres (mismo criterio que `disponibles` de CU14).
7. **Si Académico falla, la consulta falla**: 503 con el código nuevo `ERR_ESC_20` (`ServicioNoDisponibleException`, que Escuela todavía no tenía). No se devuelve un listado parcial. Si Identidad falla, cae en el 500 general como en CU09.
8. **No hay migración ni cambio de datos.** El `vehiculoCodigo` de las franjas y de las reservas sigue vacío: asociarlo es de CU14 y CU17.

## Contrato de datos / recurso HTTP

| Servicio | Método | Path | Status | `data` |
| --- | --- | --- | --- | --- |
| `escuela` | `GET` | `/api/v1/disponibilidad-instructores?page&pageSize&search&filter[fecha]&filter[momento]&filter[disponibilidad]&filter[auto]` | 200 | `DisponibilidadInstructorResponse[]` + `meta` |
| `academico` | `GET` | `/api/v1/franjas/resumen?filter[fecha]&filter[horaDesde]&filter[horaHasta]` | 200 | `ResumenFranjasInstructorResponse[]` + `meta` (una sola página) |

`filter[fecha]` es `yyyy-MM-dd` (hoy por defecto). `filter[momento]`: `MANANA`, `TARDE` o `NOCHE`. `filter[disponibilidad]`: `DISPONIBLE`, `COMPLETO`, `SIN_HORARIO` o `NO_DISPONIBLE`. `filter[auto]`: `DISPONIBLE`, `NO_DISPONIBLE` o `SIN_AUTO`. `search` busca en el nombre del instructor y en la placa, marca y modelo de sus autos. En Académico, `filter[horaDesde]` es inclusiva y `filter[horaHasta]` exclusiva (`HH:mm`).

`DisponibilidadInstructorResponse`: `instructorCodigo`, `nombreCompleto`, `estadoInstructor` (`ACTIVO`, `LICENCIA`, `INACTIVO`), `disponibilidad`, `estadoAutos`, `autos` (`{ vehiculoCodigo, placa, marca, modelo, estado }[]`), `libres`, `reservadas` y `bloqueadas`.

`ResumenFranjasInstructorResponse`: `instructorCodigo` (el `codigo_usuario`), `total`, `libres`, `reservadas` y `bloqueadas`.

Código de error nuevo: `ERR_ESC_20` ([docs/errors](../../../errors/escuela/ERRORES_ESCUELA.md)).

## Orden de ejecución

| Código | Nombre | Microservicio | Depende de |
|--------|--------|---------------|------------|
| [UT-CU11-BE-01](./UT-CU11-BE-01.md) | Resumen de franjas por instructor | `academico` | — |
| [UT-CU11-BE-02](./UT-CU11-BE-02.md) | Reglas de disponibilidad y autos vigentes por instructor | `escuela` | — |
| [UT-CU11-BE-03](./UT-CU11-BE-03.md) | Cliente de Académico y error `ERR_ESC_20` | `escuela` | 02 |
| [UT-CU11-BE-04](./UT-CU11-BE-04.md) | Caso de uso: consultar la disponibilidad | `escuela` | 02, 03 |
| [UT-CU11-BE-05](./UT-CU11-BE-05.md) | Controller de disponibilidad | `escuela` | 04 |
| [UT-CU11-BE-06](./UT-CU11-BE-06.md) | Ruta en el gateway y mapa de servicios | `gateway` | 05 |

**Cadena:** `01` y `02` en paralelo → `03 → 04 → 05 → 06`. La UT 03 necesita de 01 solo para la prueba de punta a punta; se implementa contra el contrato.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar los instructores de hoy con su disponibilidad y sus autos | 01, 02, 03, 04, 05, 06 |
| F1 | Otro día o momento del día | 01, 02, 04, 05 |
| F2 | Filtros por disponibilidad y por autos, y búsqueda | 02, 04, 05 |
| A1 | Sin resultados: listado vacío | 04 |
| A2 | Fecha o filtro inválido | 04, 05 |
| A3 | Académico no responde | 03, 04 |
| A4 | Identidad no responde o no devuelve un nombre | 04 |
| A5, A6 | Rol, token sin escuela, rate limit | 01, 05 (rol y token); el rate limit es del gateway |

## Follow-ups (no se implementan en estas UT)

- Asociar el auto a las franjas, las reservas y las clases, y mostrar los autos sin instructor (CU14 y CU17).
- Disponibilidad de un rango de días o por horas puntuales.
- Una caché corta del resumen si la consulta se vuelve pesada con muchas escuelas.

## Qué NO incluyen estas UT

- La pantalla web ([CU11-FE](../UT-FRONTEND/README.md)).
- Reservar, bloquear o desbloquear franjas, ni cambiar el estado de un instructor o de un auto.
- Cambios en el esquema de MongoDB o PostgreSQL: no hay migraciones.
- Liberar o asignar autos (CU10) ni la vista del estudiante o del instructor.
