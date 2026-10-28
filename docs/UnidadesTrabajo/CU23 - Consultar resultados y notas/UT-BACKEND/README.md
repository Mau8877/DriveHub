# Unidades de trabajo — CU23-BE

**Caso de uso:** Consultar resultados y notas: el estudiante ve sus notas y el Administrador las de su escuela.
**Microservicio(s) / micro-frontend(s):** `academico` (Spring Boot, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU23 - Consultar resultados y notas.md`](../../../CasosDeUso/CU23%20-%20Consultar%20resultados%20y%20notas.md)
**Recurso HTTP / contrato:** `/api/v1/resultados` (ya enrutado por el gateway a `academico`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Solo lectura:** la colección `resultados` ya la escriben CU21 y CU22 (con su `escuelaCodigo`); CU23 solo la consulta.
- **Dos vistas, dos rutas, un servicio:** `GET /api/v1/resultados/mios` (`ESTUDIANTE`) y `GET /api/v1/resultados` (`ADMIN_ESCUELA`). Así la decisión de rol queda en `@PreAuthorize` y no en un `if` sobre los roles del token.
- **El estudiante ve solo lo suyo:** el servicio ignora el filtro `estudianteCodigo` en `/mios`.
- **Nombres de estudiantes** unidos desde Identidad (`IdentidadClient.nombresDeEstudiantes`, de CU22) en una consulta por página, solo en la vista del Administrador.
- **`dia`** es la fecha de la nota en la hora de la escuela (la pantalla web la usa para la columna de fecha); `fecha` es el instante.
- **Índice nuevo** `idx_resultado_escuela_fecha` para la vista de la escuela.
- **Instructor pendiente:** necesita a Escuela (alumnos asignados); es seguimiento declarado en el CU.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/resultados/mios?page&pageSize&filter[origen]` | 200 | `ResultadoResponse[]` + `meta` |
| `GET` | `/api/v1/resultados?page&pageSize&filter[origen]&filter[estudianteCodigo]` | 200 | `ResultadoResponse[]` + `meta` |

`ResultadoResponse`: `codigo`, `estudianteCodigo`, `estudianteNombre` (solo del Administrador), `origen`, `titulo`, `puntaje`, `aprobado`, `fecha`, `dia`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU23-BE-01](./UT-CU23-BE-01.md) | Repositorio y servicio de resultados | — |
| [UT-CU23-BE-02](./UT-CU23-BE-02.md) | Controller y diseño de BD | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Notas del estudiante | 01, 02 |
| F1 | Notas de la escuela con nombres | 01, 02 |
| A1 a A4 | Filtro inválido, token sin escuela, Identidad, rol | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Vista del instructor (alumnos asignados, consultando a Escuela).
- La nota de la prueba de clase, cuando CU17 la escriba.

## Qué NO incluyen estas UT

- Escribir notas (CU21, CU22) ni las pantallas ([UT-FRONTEND](../UT-FRONTEND/README.md), [UT-MOBILE](../UT-MOBILE/README.md)).
