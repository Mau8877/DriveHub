# Errores — Notificaciones (`NOT`)

Códigos del microservicio Notificaciones (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `ErrorCodes` (`src/common/exceptions/error-codes.ts`).

## `ERR_NOT_01`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** Un servicio pidió una ruta interna (`/internal/v1/**`, CU36) sin la credencial de servicio en `X-Service-Key`, con una incorrecta, o el servicio no tiene credencial configurada. No se envía nada.
- **Dónde se lanza:** `CredencialServicioGuard.canActivate()`

## `ERR_NOT_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** Se quiso quitar un dispositivo push que no existe o es de otra persona (CU36). Uno ajeno y uno inexistente responden igual.
- **Dónde se lanza:** `DispositivosService.quitar()`

## `ERR_NOT_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La franja de horas de silencio de las preferencias no es válida (CU37): viene una sola de las dos horas, o las dos son iguales. El formato `HH:mm` mal escrito no llega aquí: lo rechaza la validación del cuerpo con `ERR_SYS_01`. No se cambia nada.
- **Dónde se lanza:** `PreferenciasService.guardar()`

## `ERR_NOT_04`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** Se quiso abrir o marcar como leído un aviso de la bandeja (CU38) que no existe, ya expiró (180 días) o es de otra persona. Uno ajeno, uno inexistente y uno que no es de la bandeja responden igual. No se cambia nada.
- **Dónde se lanza:** `HistorialNotificacionesService.leer()`
