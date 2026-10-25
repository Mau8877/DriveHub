# Unidades de trabajo — CU29-BE

**Caso de uso:** Consultar pagos de mis alumnos (Instructor): ver sus alumnos asignados con las horas compradas, las gastadas con él y las que les quedan, y el detalle clase a clase con los pagos del alumno.
**Microservicio(s) / micro-frontend(s):** `identidad` (hexagonal), `escuela` (clean), `pagos` (Spring Boot, capas) y `gateway` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU29 - Consultar pagos de mis alumnos`](../../../CasosDeUso/CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos.md)
**Recurso HTTP / contrato:** `/api/v1/horas-alumnos` (pagos), `/api/v1/asignaciones-alumno/mis-alumnos` (escuela) y `/api/v1/estudiantes/consulta` (identidad)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Tres servicios, una sola pantalla.** Pagos tiene las horas, Escuela sabe quiénes son los alumnos del instructor e Identidad tiene los nombres. Pagos pide a los otros dos con el Bearer del instructor y **no guarda** ni nombres ni la lista de alumnos. Una UT por servicio, cada una nombra el suyo.
2. **Paginación en memoria en Pagos.** Para buscar por nombre hace falta tener todos los alumnos con su nombre; se pide hasta un tope de 500 alumnos por instructor (mucho más de lo razonable para uno) y se pagina en Pagos. Si algún día el tope no alcanza, la búsqueda por nombre se mueve a Identidad.
3. **"Horas gastadas conmigo" sale de la billetera**, no de Académico: cada consumo guarda el `instructor_codigo` (y su devolución lo copia), así que no hace falta preguntarle nada a Académico. Esto exige que Académico lo envíe siempre ([CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md)).
4. **Horas compradas = las de los pagos confirmados**, sin los reembolsados; **horas restantes** es el saldo total del alumno, no solo lo reservado con este instructor.
5. **Solo lectura:** el instructor no confirma, revierte ni reembolsa nada. Ve plan, horas, fecha y estado de los pagos; el comprobante lo suma CU30.
6. **No se distingue "no existe" de "no es tu alumno"** (404 en ambos casos).

## Contrato de datos / recurso HTTP

Exigen `INSTRUCTOR` (`401` sin sesión, `403` sin el rol).

| Servicio | Método | Path | Query / cuerpo | Status | `data` |
| --- | --- | --- | --- | --- | --- |
| `pagos` | `GET` | `/api/v1/horas-alumnos` | `page`, `pageSize`, `search` | 200 | `HoraAlumnoResponse[]` + `meta` |
| `pagos` | `GET` | `/api/v1/horas-alumnos/{estudianteCodigo}` | — | 200 | `HorasAlumnoDetalleResponse` |
| `escuela` | `GET` | `/api/v1/asignaciones-alumno/mis-alumnos` | `page`, `pageSize` | 200 | `MiAlumnoResponse[]` + `meta` |
| `escuela` | `GET` | `/api/v1/asignaciones-alumno/mis-alumnos/{estudianteCodigo}` | — | 200 / 404 | `MiAlumnoResponse` |
| `identidad` | `POST` | `/api/v1/estudiantes/consulta` | `{ codigos }` (1 a 100) | 200 | `PersonaEstudianteResponse[]` |

`HoraAlumnoResponse`: `estudianteCodigo`, `nombres`, `apellidos`, `horasCompradas`, `horasGastadasConmigo`, `horasRestantes`. `HorasAlumnoDetalleResponse`: `resumen`, `consumos` (`claseCodigo`, `fecha`, `horas`) y `pagos` (`codigo`, `planNombre`, `horas`, `estado`, `creadoEn`). Errores: `404 ERR_PAG_12` alumno no asignado o inexistente, `503 ERR_PAG_15` Escuela o Identidad no disponibles. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU29-BE-01](./UT-CU29-BE-01.md) | Identidad: consultar estudiantes por código (nombres) | — |
| [UT-CU29-BE-02](./UT-CU29-BE-02.md) | Escuela: alumnos asignados a un instructor | — |
| [UT-CU29-BE-03](./UT-CU29-BE-03.md) | Pagos: cliente hacia Escuela y consultas de nombres a Identidad | 01, 02 |
| [UT-CU29-BE-04](./UT-CU29-BE-04.md) | Consulta de horas de los alumnos de un instructor | 03 |
| [UT-CU29-BE-05](./UT-CU29-BE-05.md) | Controller de horas de los alumnos | 04 |
| [UT-CU29-BE-06](./UT-CU29-BE-06.md) | Gateway: ruta de horas de los alumnos | 05 |

**Cadena:** `01, 02 → 03 → 04 → 05 → 06` (la 01 y la 02 son de servicios distintos y pueden hacerse en paralelo).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 a 3, CA01, CA02 | Listado con horas y búsqueda por nombre | 01, 02, 03, 04, 05 |
| F1, CA03, CA04 | Detalle con el tiempo gastado con él y los pagos del alumno | 04, 05 |
| A2, CA05 | Alumno no asignado, inexistente o de otra escuela | 02, 04, 05 |
| A3 | Alumno sin clases: ceros y lista vacía | 04 |
| A1, CA07 | Instructor sin alumnos | 02, 04 |
| A5, CA08 | Solo instructor | 01, 02, 05 |
| A6 | Rate limit | ya cubierto por el gateway; sin UT nueva |

El comprobante (F2, A4, CA06) es de [CU30](../../CU30%20-%20Generar%20comprobante/UT-BACKEND/README.md). La pantalla está en [UT-MOBILE](../UT-MOBILE/README.md).

## Follow-ups (no se implementan en estas UT)

- Mover la búsqueda por nombre a Identidad si un instructor llega a tener más de 500 alumnos.
- Mostrar la fecha y hora reales de cada clase (hoy se muestra la del consumo): requiere que Académico las informe.
- Una vista del Administrador con todos los alumnos y sus horas.

## Qué NO incluyen estas UT

- Crear o cambiar asignaciones de alumnos (CU10).
- Acciones sobre pagos desde esta pantalla.
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01), `escuela` (UT 02), `pagos` (UT 03 a 05) y `gateway` (UT 06). Los cambios de `.claude/` son `[REPO]` y los de `docs/` `[DOCS]`.
