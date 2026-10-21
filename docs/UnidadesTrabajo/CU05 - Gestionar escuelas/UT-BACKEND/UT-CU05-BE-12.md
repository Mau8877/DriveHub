# UT-CU05-BE-12 — Endpoints de gestión de escuelas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-12` |
| **Nombre** | Endpoints de gestión de escuelas |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-08, UT-CU05-BE-09, UT-CU05-BE-10, UT-CU05-BE-11 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La pantalla de gestión de escuelas necesita una API.

## Objetivo

El Super admin dispone de los endpoints de gestión del recurso `escuelas`.

## Alcance

- `presentation/controller/EscuelaGestionController` (`/api/v1/escuelas`, junto al `EscuelaController` público existente, que no cambia): `POST` → `201`, `data`: `EscuelaCreadaResponse`; `GET` con `page`, `pageSize`, `search` y `filter[estado]` (`ACTIVA` o `INACTIVA`, se lee con `@RequestParam(name = "filter[estado]")`) → `200`, `data` array y `meta` (`ApiResponse.successList` + `PageMeta`); `PATCH /{codigo}` con `{ nombre }` → `200`, `data`: `EscuelaResponse`; `DELETE /{codigo}` → `200`, `data: null`; `POST /{codigo}/reactivar` → `200`, `data`: `EscuelaResponse`.
- Requests/responses HTTP en `presentation/controller/`: `CrearEscuelaRequest` (`nombre` `@NotBlank` `@Size(3,120)`, `slug` `@Pattern` + `@Size(3,60)`, `administrador` `@Valid` con nombres, apellidos, fechaNacimiento, email, password de 9 caracteres como mínimo, telefono opcional), `EditarEscuelaRequest`, `EscuelaResponse`, `EscuelaListadaResponse`, `EscuelaCreadaResponse`. Los Request HTTP son distintos de los DTO de `application/dto/`.
- `SecurityConfig`: sin cambios esperados. `GET /api/v1/escuelas/*` (consulta por slug) sigue público porque solo cubre un segmento; `GET /api/v1/escuelas` (listado), `POST`, `PATCH`, `DELETE` y `POST .../reactivar` caen en `anyRequest().authenticated()`. La autorización por rol está en los casos de uso. Si el test de seguridad muestra lo contrario, corregir los `requestMatchers`.
- `@WebMvcTest` con los casos de uso mockeados (importando `SecurityConfig`): sin token → 401 y con `ADMIN_ESCUELA` → 403 en cada endpoint; sobre estándar y códigos HTTP; validación de campos → 400 `ERR_SYS_01`; `ERR_ESC_03`, `ERR_ESC_04`, `ERR_ESC_05`, `ERR_ESC_06` y los `ERR_IDE_01` e `ERR_IDE_02` que llegan de Identidad con su HTTP; el listado trae `meta`; `GET /api/v1/escuelas/{slug}` sigue respondiendo sin token.
- `.claude/rules/MICROSERVICIOS.md` y el gateway **no** cambian: `escuelas` ya es un recurso registrado.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaGestionController.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/CrearEscuelaRequest.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EditarEscuelaRequest.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaResponse.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaListadaResponse.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaCreadaResponse.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/presentation/controller/EscuelaGestionControllerTest.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/config/SecurityConfig.java (solo si hace falta)`

## Fuera de alcance (no tocar)

- Lógica de negocio, acceso a repositorios, `try/catch` y logging en el controller.
- Datos del Administrador en el listado o en `EscuelaResponse`.
- El gateway (UT 13).

## Resultado esperado

Los endpoints de gestión de escuelas responden en el sobre estándar y solo los puede usar el Super admin.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado y el token de `superadmin@gmail.com`: crear una escuela con su Administrador, listarla, editarle el nombre, desactivarla (el login de su Administrador responde 403 `ERR_IDE_07`) y reactivarla (vuelve a entrar). Con `ERR_ESC_03` al repetir el slug y `ERR_IDE_01` al repetir el correo no queda ni escuela ni cuenta nuevas.
