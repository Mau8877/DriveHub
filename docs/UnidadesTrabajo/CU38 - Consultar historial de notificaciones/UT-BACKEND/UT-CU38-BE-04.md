# UT-CU38-BE-04 — Endpoints de la bandeja

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-BE-04` |
| **Nombre** | Endpoints de la bandeja |
| **Caso de uso** | CU38-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU38-BE-03 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Ver `preferencias.controller.ts` y su spec como modelo.

## Solicitud de negocio

La app necesita leer la bandeja, el contador y marcar avisos por HTTP.

## Objetivo

Estudiantes e instructores usan los cuatro endpoints de `/api/v1/notificaciones`.

## Alcance

- `controllers/notificaciones.controller.ts` (`@Controller('api/v1/notificaciones')`, `@UseGuards(JwtAuthGuard, RolesGuard)`, `@Roles('ESTUDIANTE', 'INSTRUCTOR')`): controller delgado, toma el código de `@UsuarioActual()`.
  - `GET /` → `ApiResponses.successList(items, mensaje, meta)`.
  - `GET /conteo-no-leidas`, `POST /leer-todas` y `POST /:codigo/leer` → `ApiResponses.success`. Declarar las rutas fijas antes de `:codigo`.
- `dto/consultar-notificaciones.query.ts`: `page` (entero ≥ 1, por defecto 1), `pageSize` (entero de 1 a 100, por defecto 20), `filter[leida]` (`true`/`false`, convertido a boolean) y `filter[tipo]` (valor de `TipoNotificacion`). Cualquier otro valor es 400 `ERR_SYS_01` por `ValidationPipe`. Express 5 trae el parser de query simple: `main.ts` fija `app.set('query parser', 'extended')` para que `filter[campo]` llegue como objeto `filter` (el test del controller lo fija igual).
- `NotificacionesModule`: suma el controller. El `SecurityModule` ya está importado.
- Tests del controller (`@nestjs/testing` + supertest, service falso): el sobre con `status: success`, `data` y `meta` en el listado; los valores por defecto de `page`/`pageSize`; los filtros llegan al service; usan el código del token; 401 `ERR_SYS_04` sin token; 403 `ERR_SYS_05` con otro rol; 400 `ERR_SYS_01` con `pageSize=0`, `filter[leida]=quizas` o `filter[tipo]=OTRO`; 404 `ERR_NOT_04` cuando el service lo lanza; `/leer-todas` no se confunde con `/:codigo/leer`.
- Una línea en el `README.md` del servicio con los recursos nuevos.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/controllers/notificaciones.controller.ts
- projects/backend/notificaciones/src/features/notificaciones/dto/consultar-notificaciones.query.ts
- projects/backend/notificaciones/src/features/notificaciones/notificaciones.module.ts
- projects/backend/notificaciones/src/main.ts
- projects/backend/notificaciones/src/features/notificaciones/tests/notificaciones.controller.spec.ts
- projects/backend/notificaciones/README.md

## Fuera de alcance (no tocar)

- El gateway y `MICROSERVICIOS.md`: `notificaciones` ya está registrado en los dos.
- `NotificacionesInternasController` (`/internal/v1/notificaciones`, CU36): es otra ruta y otra credencial.
- La lógica del service y las consultas (UT-CU38-BE-02 y 03).

## Resultado esperado

Con el stack arriba, un aviso enviado por `POST /internal/v1/notificaciones` aparece en `GET /api/v1/notificaciones` de esa persona como no leído; al abrirlo con `POST .../leer` queda leído y el conteo baja.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde; el test de `AppModule` sigue pasando.
2. Con el stack, por el gateway y con un JWT de estudiante (`filter%5Bleida%5D=false` codificado): enviar dos avisos, listar (2, no leídos), `conteo-no-leidas` (2), `leer` uno (el conteo pasa a 1), `leer-todas` (`marcadas: 1`) y repetir `leer` (200, sin cambios). Con el JWT de otra persona, `leer` de ese código responde 404 `ERR_NOT_04`.
