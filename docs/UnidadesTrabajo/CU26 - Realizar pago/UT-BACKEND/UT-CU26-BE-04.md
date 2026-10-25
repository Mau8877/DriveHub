# UT-CU26-BE-04 — Planes disponibles para el estudiante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-04` |
| **Nombre** | Planes disponibles para el estudiante |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante tiene que ver qué planes puede comprar: los activos de su escuela, del más barato al más caro.

## Objetivo

`GET /api/v1/planes-pago/disponibles` devuelve los planes activos de la escuela del estudiante, paginados.

## Alcance

- `PlanPagoSpecification` ya trae `deLaEscuela` y `conEstado`; se reutilizan con `conEstado(true)`.
- `PlanDisponibleResponse(codigo, nombre, horas, costoBs, costoUsd)` (`record`; sin `activo` ni `creadoEn`).
- `PlanPagoService.disponibles(escuelaCodigo, page, pageSize)` con `@PreAuthorize("hasRole('ESTUDIANTE')")` y `@Transactional(readOnly = true)`: `page` 1, `pageSize` 20 y máximo 100; orden `horas ASC, costo_bs ASC, id ASC`; el dólar sale de `ConversionMoneda`; devuelve la página y su `PageMeta`.
- `PlanPagoController`: `GET /api/v1/planes-pago/disponibles` con `@CurrentEscuelaId`. Va declarado **antes** del `GET /{codigo}` para que la ruta literal no se tome como un código.
- Tests: service con el repositorio mockeado (activos solo, orden, valores por defecto, tope de 100); `PlanPagoRepositoryTest` suma el caso del orden por horas con planes de dos escuelas y desactivados; `@WebMvcTest` del endpoint (sobre `ApiResponse` con `data` array y `meta`, 401 sin sesión, 403 para `ADMIN_ESCUELA`); el Administrador sigue viendo todos sus planes en `GET /api/v1/planes-pago` (regresión).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/dto/PlanDisponibleResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/service/PlanPagoService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/controller/PlanPagoController.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoRepositoryTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoControllerTest.java

## Fuera de alcance (no tocar)

- Comprar el plan (UT 05).
- Cambiar lo que ve el Administrador en CU25.
- Desactivados: nunca se ofrecen.

## Resultado esperado

Un estudiante ve solo los planes activos de su escuela, los más baratos primero; un Administrador recibe 403 en este endpoint.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el estudiante de ejemplo, `GET /api/v1/planes-pago/disponibles` lista los 5 planes del seeder; tras desactivar uno desde CU25, ya no aparece.
