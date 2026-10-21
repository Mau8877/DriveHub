# UT-CU06-BE-10 — Confirmar el cambio de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-10` |
| **Nombre** | Confirmar el cambio de correo |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-01, UT-CU06-BE-05 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Al abrir el enlace del correo nuevo, la cuenta pasa a usar esa dirección, ya verificada, y el correo anterior se entera.

## Objetivo

`ConfirmarCambioCorreoUseCase` y su servicio.

## Alcance

- `domain/port/in/ConfirmarCambioCorreoUseCase`: `void confirmar(String token)`.
- `application/service/ConfirmarCambioCorreoService`, `@Transactional(noRollbackFor = BusinessException.class)` (mismo patrón que `ConfirmarCorreoService`):
  1. busca el enlace por `hashear(token)`; debe ser de tipo `CAMBIO_CORREO`, no usado y no vencido; si no → `ValidationException` `ERR_IDE_11` (el mismo de CU04 y CU04b);
  2. la cuenta debe existir y no estar `INACTIVA`; si no → `ERR_IDE_11`;
  3. se vuelve a comprobar que el `correoNuevo` no sea de otra cuenta (otra persona pudo registrarlo mientras tanto) → `ERR_IDE_11`;
  4. marca el enlace usado e invalida los pendientes de `CAMBIO_CORREO`;
  5. actualiza la cuenta: `email = correoNuevo`, `correoVerificado = true` (las sesiones no se tocan);
  6. envía el aviso al correo **anterior** con `EnviadorCorreoPort.enviarAvisoCambioCorreo`; si falla, `WARN` y el cambio ya hecho se conserva.
- Logs: `INFO` con `module="auth"` y `usuarioCodigo`; en los rechazos, `WARN` con `errorCode`. Nunca el token ni las direcciones completas.
- Tests unitarios: confirma, cambia el correo y lo deja verificado, consume el enlace y envía el aviso al anterior; un enlace de `VERIFICACION` o `RECUPERACION` → `ERR_IDE_11`; inexistente, usado o vencido → `ERR_IDE_11`; correo ya tomado → `ERR_IDE_11` sin cambiar nada; si falla el aviso, igual queda cambiado; cuenta inactiva → `ERR_IDE_11`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ConfirmarCambioCorreoUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ConfirmarCambioCorreoService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/ConfirmarCambioCorreoServiceTest.java`

## Fuera de alcance (no tocar)

- Pedir el cambio (UT 09) y el endpoint público (UT 13).
- Cerrar sesiones por el cambio de correo (el CU no lo pide).
- Modificar `ConfirmarCorreoService` (CU04b).

## Resultado esperado

Un enlace válido cambia el correo de la cuenta, lo deja verificado y avisa al correo anterior.

## Validación

1. `./mvnw test` en `identidad` pasa con `ConfirmarCambioCorreoServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
