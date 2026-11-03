# UT-CU37-BE-03 — Endpoints `GET` y `PUT`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU37-BE-03` |
| **Nombre** | Endpoints `GET` y `PUT` |
| **Caso de uso** | CU37-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU37-BE-01, UT-CU37-BE-02 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app necesita leer y guardar las preferencias de la persona.

## Objetivo

La persona con rol Estudiante o Instructor consulta y guarda sus preferencias por HTTP.

## Alcance

- `controllers/preferencias.controller.ts`: `GET` y `PUT` en `api/v1/preferencias-notificacion`, con `@UseGuards(JwtAuthGuard, RolesGuard)` y `@Roles('ESTUDIANTE', 'INSTRUCTOR')`. Controller delgado: toma el `codigo` de `@UsuarioActual()`, llama a un método del service y responde con `ApiResponses.success`.
- `PreferenciasModule`: suma el controller y el service; importa `SecurityModule` y se registra en `app.module.ts`.
- Tests del controller (`@nestjs/testing` + supertest, service falso): `GET` y `PUT` devuelven el sobre con `status: success` y los datos; usan el `codigo` del token (no uno del cuerpo); 401 `ERR_SYS_04` sin token; 403 `ERR_SYS_05` con otro rol; 400 `ERR_SYS_01` con un booleano que falta o una hora mal escrita; 400 `ERR_NOT_03` cuando el service lo lanza.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/preferencias/controllers/preferencias.controller.ts
- projects/backend/notificaciones/src/features/preferencias/preferencias.module.ts
- projects/backend/notificaciones/src/features/preferencias/tests/preferencias.controller.spec.ts
- projects/backend/notificaciones/src/app.module.ts
- projects/backend/notificaciones/README.md (una línea con el recurso)

## Fuera de alcance (no tocar)

- Registrar el recurso en el gateway y en `MICROSERVICIOS.md`: `preferencias-notificacion` ya está en los dos.
- El endpoint del historial (CU38).

## Resultado esperado

Con el stack arriba, `PUT` con un JWT de estudiante guarda las preferencias y el siguiente `GET` las devuelve; el siguiente aviso de CU36 las respeta.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde; el test del `AppModule` sigue pasando.
2. Con el stack: `PUT` desactivando `PAGO`, y un `POST /internal/v1/notificaciones` de tipo `PAGO` a esa persona responde `OMITIDA`.
