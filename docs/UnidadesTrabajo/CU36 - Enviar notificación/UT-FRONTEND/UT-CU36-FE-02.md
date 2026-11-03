# UT-CU36-FE-02 — Registrar al iniciar sesión y quitar al cerrarla

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-FE-02` |
| **Nombre** | Registrar al iniciar sesión y quitar al cerrarla |
| **Caso de uso** | CU36-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU36-FE-01, UT-CU36-BE-09 |

## Antes de implementar

Leer [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (el access token vive solo en memoria del shell).

## Solicitud de negocio

El navegador debe empezar a recibir avisos cuando la persona entra, y dejar de recibirlos cuando sale (otra persona podría usar el mismo equipo).

## Objetivo

La sesión del shell activa el push al tener sesión y lo desactiva al cerrarla.

## Alcance

- `ShellLayout` (solo existe con sesión): un `effect` que llama `PushWebService.activar()` cuando la persona está autenticada. Sin esperar su resultado.
- `SesionService.cerrarSesion()`: antes de `limpiar()`, `void push.desactivar()`: la petición sale ya con el token y el cierre no espera ni depende de su resultado.
- Tests: `cerrarSesion` llama a `desactivar` antes de limpiar y sigue aunque falle; el layout llama a `activar` al haber sesión.

Archivos esperados (crear/tocar):

- projects/frontend/shell/src/app/layout/shell-layout.ts (+ spec)
- projects/frontend/shell/src/app/core/sesion/sesion.service.ts (+ spec)

## Fuera de alcance (no tocar)

- Quitar el dispositivo cuando la sesión vence sin cerrarla a mano (se reutiliza el mismo token al volver a entrar).
- El TODO de la campana de avisos en `shell-layout.ts` (CU38).

## Resultado esperado

Entrar al panel registra el navegador (si hay permiso y configuración) y cerrar sesión lo quita.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build`.
2. Manual: entrar, aceptar el permiso, ver el dispositivo `WEB` en `DispositivosPush`; cerrar sesión y verlo desaparecer.
