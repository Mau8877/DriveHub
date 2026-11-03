# UT-CU37-BE-02 — Guardar y consultar las preferencias

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU37-BE-02` |
| **Nombre** | Guardar y consultar las preferencias |
| **Caso de uso** | CU37-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU37-BE-01 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#dynamodb), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Lo que la persona elige tiene que quedar guardado y tiene que ser lo mismo que CU36 aplica al enviar.

## Objetivo

`PreferenciasService` consulta (con valores por defecto) y guarda la configuración completa de una persona.

## Alcance

- `features/preferencias/models/preferencias-por-defecto.ts`: constante `PREFERENCIAS_POR_DEFECTO` (todos los canales y tipos en `true`, sin silencio). `features/notificaciones/services/politica-envio.ts` (de CU36) pasa a usarla en lugar de sus valores escritos a mano; su comportamiento y sus tests no cambian.
- `PreferenciasNotificacion.email` pasa a opcional (`email?: string`) en el model.
- `repositories/preferencias.repository.ts`: se suma `guardar(preferencias)` (`PutCommand`) a `obtener`.
- `dto/preferencias.request.ts` (`PreferenciasRequest`, con `canales` y `tipos` anidados y validados con `class-validator`/`class-transformer`: booleanos obligatorios, `silencioDesde` y `silencioHasta` con patrón `^([01]\d|2[0-3]):[0-5]\d$`, `email` con `@IsEmail`) y `dto/preferencias.response.ts`.
- `services/preferencias.service.ts`:
  - `obtener(usuarioCodigo)`: lo guardado, o los valores por defecto con `actualizadoEn: null`, sin escribir nada.
  - `guardar(usuarioCodigo, request)`: valida la franja (las dos o ninguna, no iguales; si no, `ValidationException('ERR_NOT_03', ...)`), conserva el `codigoPreferenciasNotificacion` existente o crea uno (UUID v7), fija `actualizadoEn` y guarda. Loguea INFO con `usuarioCodigo` (sin el correo).
- Tests del service con el repositorio falso: valores por defecto sin escribir; guardar crea con código nuevo; guardar de nuevo conserva el código; sin franja deja el silencio vacío aunque antes lo hubiera; una sola hora y horas iguales dan `ERR_NOT_03`; el correo no se loguea. Tests del DTO: horas mal escritas, `24:00`, `9:5`, y un correo inválido, dan error de validación.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/preferencias/{dto,services}/*, models/preferencias-por-defecto.ts
- projects/backend/notificaciones/src/features/preferencias/models/preferencias-notificacion.model.ts
- projects/backend/notificaciones/src/features/preferencias/repositories/preferencias.repository.ts
- projects/backend/notificaciones/src/features/preferencias/tests/*.spec.ts
- projects/backend/notificaciones/src/features/notificaciones/services/politica-envio.ts

## Fuera de alcance (no tocar)

- Los endpoints (UT 03).
- Cualquier cambio en cómo CU36 decide los canales (`decidirCanales` no cambia de comportamiento).
- Eventos de Identidad para el correo.

## Resultado esperado

`guardar` seguido de `obtener` devuelve la misma configuración; sin guardar, `obtener` devuelve los valores por defecto.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde (los tests de CU36 siguen pasando).
