# UT-CU05-BE-06 — Mensaje de escuela inactiva al iniciar y renovar sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-06` |
| **Nombre** | Mensaje de escuela inactiva al iniciar y renovar sesión |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cuando el Super admin desactiva una escuela, su Administrador debe ver con claridad que la escuela está inactiva al intentar entrar.

## Objetivo

El rechazo del login y de la renovación dice que la escuela está inactiva.

## Alcance

- `SesionEmisora.escuelaSuspendida()`: el mensaje pasa de "La escuela de tu cuenta está suspendida" a **"Tu escuela está inactiva. Contacta al administrador de la plataforma"**. El código `ERR_IDE_07` y el nombre de la constante `ESCUELA_SUSPENDIDA` **no cambian** (los códigos no se renumeran).
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`: el significado de `ERR_IDE_07` pasa a "La escuela de la cuenta está inactiva (desactivada por el Super admin) o ya no existe".
- Tests de `IniciarSesionService` y `RenovarSesionService` que comprueban el texto.
- Verificar (sin cambiar código si ya funciona) que una escuela con borrado lógico le llega a `SesionEmisora` como `EscuelaRef` con `activa = false` gracias a la UT 01 de `escuela`, y que `RenovarSesionService` también la rechaza.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/SesionEmisora.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/IniciarSesionServiceTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/RenovarSesionServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`

## Fuera de alcance (no tocar)

- Cambiar quién queda bloqueado: sigue siendo toda cuenta de la escuela (la comprobación ya existe).
- Cortar sesiones abiertas al instante: la sesión vigente deja de servir cuando vence su token de acceso o intenta renovarse.
- El texto en la pantalla de login: el shell ya muestra el mensaje del backend.

## Resultado esperado

Quien intenta entrar a una escuela inactiva lee que su escuela está inactiva.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con una escuela con `deleted_at` en la base (requiere la UT 01), `POST /api/v1/auth/login` de su Administrador responde 403 `ERR_IDE_07` con el mensaje nuevo, y también la renovación.
