# UT-CU02-BE-03 — Endpoint `GET /api/v1/escuelas/por-codigo/{codigo}`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-03` |
| **Nombre** | Endpoint `GET /api/v1/escuelas/por-codigo/{codigo}` |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU02-BE-02 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

Identidad consulta el estado de la escuela de una cuenta al iniciar y renovar sesión.

## Objetivo

Existe `GET /api/v1/escuelas/por-codigo/{codigo}`, accesible sin token para la llamada interna entre servicios.

## Alcance

- `EscuelaController`: método `GET /por-codigo/{codigo}` que llama a `ObtenerEscuelaPorCodigoUseCase` y responde `200` con `EscuelaEstadoResponse` (`record`: `codigo`, `slug`, `nombre`, `activa`) en el sobre estándar; `404 ERR_ESC_02` si no existe. El `codigo` mal formado (no UUID) responde `400 ERR_SYS_01`.
- `SecurityConfig`: `permitAll` para `GET /api/v1/escuelas/por-codigo/*`. Se llama solo entre servicios por la red interna; el gateway **no** lo deja pasar sin token (UT 13).
- `@WebMvcTest`: `200` con la forma del sobre (incluido `activa=false`), `404 ERR_ESC_02`, `400` con un código no UUID, accesible sin autenticación.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaController.java`, `EscuelaEstadoResponse.java`
- `.../infrastructure/config/SecurityConfig.java`
- `.../presentation/controller/EscuelaControllerTest.java`

## Fuera de alcance (no tocar)

- La validación de JWT del servicio (UT 14): este endpoint seguirá siendo público dentro de la red interna.
- La ruta del gateway: `/api/v1/escuelas/**` ya existe.
- Endpoints de listar, crear o editar escuelas.

## Resultado esperado

`GET http://localhost:8082/api/v1/escuelas/por-codigo/{codigo}` responde con el estado de la escuela sin token.

## Validación

1. `./mvnw test` en `escuela` pasa.
2. Con el servicio y el seeder: `curl http://localhost:8082/api/v1/escuelas/por-codigo/0198f0a0-0000-7000-8000-000000000001` devuelve `200` con `activa: true`.
3. `./mvnw checkstyle:check` sin errores.
