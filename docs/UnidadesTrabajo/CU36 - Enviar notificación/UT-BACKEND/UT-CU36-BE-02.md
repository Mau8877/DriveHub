# UT-CU36-BE-02 — Autenticación de la persona con el JWT de Identidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-02` |
| **Nombre** | Autenticación de la persona con el JWT de Identidad |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-01 |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (sección de servicios no Spring que validan el JWT) y [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md).

## Solicitud de negocio

Cada persona solo puede registrar y quitar sus propios dispositivos, así que el servicio necesita saber quién es sin confiar en el gateway.

## Objetivo

Un guard valida el access token (RS256, JWKS de Identidad) y deja disponible a la persona autenticada en el controller.

## Alcance

- `common/security/verificador-jwt.ts`: con `jose` (`createRemoteJWKSet` contra `${IDENTIDAD_URL}/.well-known/jwks.json`, emisor `drivehub-identidad`). Devuelve `UsuarioAutenticado { codigo (sub), roles, escuelaCodigo? }` o `null` ante cualquier problema.
- `common/security/jwt-auth.guard.ts`: lee `Authorization: Bearer`, y si el token no es válido lanza `UnauthorizedException('ERR_SYS_04', 'Tu sesión no es válida o venció')`. Deja el usuario en `request.usuario`.
- `common/security/usuario-actual.decorator.ts`: `@UsuarioActual()` devuelve esa persona.
- `common/security/security.module.ts`: provee y exporta el verificador y los dos guards.
- Tests con un JWKS local generado en el test: token válido, vencido, de otro emisor, con mala firma y sin header.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/common/security/verificador-jwt.ts (+ `.spec.ts`)
- projects/backend/notificaciones/src/common/security/jwt-auth.guard.ts
- projects/backend/notificaciones/src/common/security/usuario-actual.decorator.ts
- projects/backend/notificaciones/src/common/security/security.module.ts

## Fuera de alcance (no tocar)

- Autorización por rol (ningún endpoint de CU36 la pide).
- Endpoints protegidos (UT 03).
- Cualquier cambio en Identidad.

## Resultado esperado

Un endpoint con `@UseGuards(JwtAuthGuard)` responde 401 `ERR_SYS_04` sin un token válido y conoce el `codigo` de la persona con uno válido.

## Validación

1. Tests del verificador en verde.
2. `pnpm test`, `lint` y `build` en verde.
