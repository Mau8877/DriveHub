# UT-CU25-BE-05 — Controller, errores documentados y seed

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-05` |
| **Nombre** | Controller, errores documentados y seed |
| **Caso de uso** | CU25-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU25-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, `filter[...]`), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (cada código nuevo se documenta en el mismo cambio) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `academico`: `PreguntaController`, `PreguntaSeeder` y sus tests.

## Solicitud de negocio

La web necesita una API para gestionar los planes de la escuela de quien inició sesión, los errores nuevos tienen que quedar documentados, y el desarrollo necesita planes de ejemplo.

## Objetivo

`/api/v1/planes-pago` expuesto con el sobre estándar, los cuatro códigos `ERR_PAG` documentados y cinco planes de ejemplo para `escuelaexample`.

## Alcance

- `features/planes/controller/PlanPagoController` delgado, sin lógica ni `try/catch` ni logs (seis endpoints, ver el [contrato](./README.md#contrato-de-datos--recurso-http)): valida el cuerpo con `@Valid`, toma la escuela con `@CurrentEscuelaId` (un método privado `escuelaDe(UUID)` lanza `ForbiddenException` `ERR_PAG_04` si es `null`), llama a **un** método del service y envuelve el resultado en `ApiResponse` (`201` al crear, `successList` con `PageMeta` en el listado); `filter[activo]` con `@RequestParam(name = "filter[activo]", required = false) Boolean`.
- Documentación: crear `docs/errors/pagos/ERRORES_PAGOS.md` con `ERR_PAG_01` a `ERR_PAG_04` (HTTP, excepción, significado y dónde se lanza, con el formato de la guía) y sumar los cuatro links a `docs/errors/README.md` en una sección nueva `### Pagos (PAG)`; la fila de `PAG` de la tabla de prefijos deja de decir "sin errores todavía".
- `config/PlanPagoSeeder` (`@Component`, `@Profile("dev")`, `ApplicationRunner`, idempotente por nombre dentro de la escuela): cinco planes de `escuelaexample` (el código fijo `0198f0a0-0000-7000-8000-000000000001`) para comprar 1, 5, 7, 10 y 15 horas: "Clase suelta (1 hora)" 60 Bs, "Plan 5 horas" 275 Bs, "Plan 7 horas" 370 Bs, "Plan 10 horas" 500 Bs y "Plan 15 horas" 700 Bs (el precio por hora baja cuanto más horas se compran). Se crean de más a menos horas para que el listado (del más reciente al más antiguo) los muestre de menos a más.
- Tests:
  - `PlanPagoControllerTest` (`@WebMvcTest`, servicio mockeado): forma del sobre y status de cada endpoint (`201` al crear), `filter[activo]` y la paginación llegan al service, cuerpo inválido o ilegible → 400 `ERR_SYS_01`, código que no es UUID → 400, token sin escuela → 403 `ERR_PAG_04`, sin token → 401.
  - `GestionPlanesPagoIntegrationTest` (Testcontainers, `MockMvc` con JWT de prueba, un método por criterio de aceptación): CA01 (solo los de su escuela, con `costoUsd`), CA02, CA03 (incluye otra capitalización, un plan desactivado y el mismo nombre en otra escuela), CA04, CA05 (cambiar el tipo de cambio cambia `costoUsd` sin tocar los datos), CA06, CA07, CA08, CA09 y CA10 (Estudiante e Instructor → 403, sin token → 401).
  - `PlanPagoSeederTest` (sin planes crea los cinco de `escuelaexample`, activos y con el precio por hora decreciente; con ellos no crea nada).

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/controller/PlanPagoController.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PlanPagoSeeder.java`
- `docs/errors/pagos/ERRORES_PAGOS.md` y `docs/errors/README.md`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoControllerTest.java`, `GestionPlanesPagoIntegrationTest.java` y `.../config/PlanPagoSeederTest.java`

## Fuera de alcance (no tocar)

- El gateway: `planes-pago` ya está enrutado a `pagos` (predicado `Path=/api/v1/planes-pago/**`) y figura en `MICROSERVICIOS.md`.
- Cualquier cambio de código o de datos de `inscripcion_plan`, cuotas o pagos.
- La pantalla (UT-FRONTEND).

## Resultado esperado

Con el stack levantado, un Administrador crea, lista, edita, desactiva y reactiva planes de su escuela con su costo en dólares; otra escuela no ve nada de lo suyo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con `docker compose up -d pagos` y por el gateway con `adminexample@gmail.com`: el listado trae los cinco planes de ejemplo, crear con nombre repetido (en otra capitalización) responde 409 `ERR_PAG_01`, horas 0 o 501 responde 400 `ERR_SYS_01`, desactivar dos veces responde 409 `ERR_PAG_03`, un código inexistente 404 `ERR_PAG_02`, sin token 401 y `superadmin@gmail.com` 403.
