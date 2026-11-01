# UT-CU11-BE-01 — Resumen de franjas por instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-01` |
| **Nombre** | Resumen de franjas por instructor |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: el filtro `deletedAt: null` es explícito) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Como referencia, `FranjaDisponibleService`, `FranjaRepositoryImpl.disponibles` y `FranjaController` de CU14.

## Solicitud de negocio

La escuela necesita saber cuántas franjas libres, reservadas y bloqueadas tiene cada instructor un día determinado, sin pedirlas una por una.

## Objetivo

Existe `GET /api/v1/franjas/resumen`: para un día y una ventana de horas, devuelve por cada instructor de la escuela del token cuántas franjas tiene en cada estado.

## Alcance

- `FranjaRepositoryCustom.delDia(UUID escuelaCodigo, LocalDate fecha, String horaDesde, String horaHasta)` y su implementación con `MongoTemplate`: las franjas de esa escuela y esa fecha, sin borrar (`deletedAt: null`), cuya `horaInicio` está entre `horaDesde` (inclusive) y `horaHasta` (exclusive); cada límite es opcional. Sin lógica.
- `features/horarios/dto/ResumenFranjasInstructorResponse`, un `record`: `instructorCodigo`, `total`, `libres`, `reservadas`, `bloqueadas`.
- `features/horarios/service/FranjaResumenService.resumirDelDia(escuelaCodigo, fecha, horaDesde, horaHasta)` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`:
  - Sin escuela en el token, `ForbiddenException` `ERR_ACA_16` (igual que `FranjaDisponibleService`).
  - Hora con formato distinto de `HH:mm`, o `horaDesde` posterior o igual a `horaHasta`, `ValidationException` `ERR_SYS_01`.
  - `fecha` nula = hoy, con el `Clock` del servicio.
  - Agrupa por instructor: `total` son todas sus franjas de la ventana; `libres`, las `LIBRE` que todavía no empezaron (de un día posterior a hoy, o de hoy con `horaInicio` posterior a la hora actual, `HH:mm`); `reservadas` y `bloqueadas`, por estado. Solo vienen los instructores con al menos una franja, ordenados por `instructorCodigo`.
  - Un `INFO` no hace falta (es una lectura, ver LOGGING_BACKEND.md); nunca se escribe el token.
- `FranjaController`: `GET /api/v1/franjas/resumen` con `filter[fecha]`, `filter[horaDesde]` y `filter[horaHasta]` (la escuela sale de `@CurrentEscuelaId`). Sale sin paginar, con `meta` de una sola página, como `/franjas/instructores`. El recurso `franjas` ya está en el gateway.
- Tests: `FranjaResumenServiceTest` (con el repositorio mockeado y un `Clock` fijo: conteos por estado, libres sin las que ya empezaron hoy, instructor solo con franjas pasadas, ventana de horas, sin escuela, hora inválida, escuela sin franjas), un caso en `FranjaRepositoryTest` (Testcontainers; `delDia` filtra por escuela, fecha y ventana, y no trae borradas) y casos en `FranjaControllerTest` (forma del sobre con `meta`, `401` sin token, `403` sin escuela).

Archivos esperados (crear/tocar): `FranjaRepositoryCustom`, `FranjaRepositoryImpl`, `ResumenFranjasInstructorResponse`, `FranjaResumenService`, `FranjaController` y los tres tests.

## Fuera de alcance (no tocar)

- `/franjas/disponibles`, `/franjas/instructores` y el resto de la vista del estudiante (CU14).
- El modelo `Franja` y sus índices: no cambia; `vehiculoCodigo` sigue vacío.
- Las franjas del propio instructor (`GET /franjas`) y cualquier cambio de estado.
- Nombres de instructores: el resumen devuelve solo códigos.
- Códigos de error nuevos y el gateway.

## Resultado esperado

Con un token de Administrador, Académico responde en una sola consulta cuántas franjas tiene cada instructor de su escuela en un día, sin tocar nada y sin mostrar las de otra escuela.

## Validación

1. `./mvnw test -Dtest='FranjaResumenServiceTest,FranjaControllerTest,FranjaRepositoryTest'` en verde (el de repositorio queda `SKIPPED` en Windows sin Docker).
2. `./mvnw checkstyle:check` sin errores.
3. Con el servicio levantado, `GET /api/v1/franjas/resumen?filter%5Bfecha%5D=<hoy>` con el token de `adminexample@gmail.com` responde `200` con `instructorCodigo` y los conteos; con el de `instructorexample@gmail.com`, `403`.
