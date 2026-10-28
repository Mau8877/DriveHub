# Errores — Asistente IA (`IA`)

Códigos del microservicio Asistente IA (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `app/common/error_codes.py`.

## `ERR_IA_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La miniatura de la ayuda del tablero no es válida (CU40): no es una imagen JPEG o PNG
  (se comprueba por los primeros bytes del archivo, no por el nombre) o pesa más de 500 KB. No se guarda nada.
- **Dónde se lanza:** `AyudaTableroService._validar_miniatura()`, desde `AyudaTableroService.crear()`

## `ERR_IA_02`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** No se pudo guardar la miniatura en MinIO (el almacén de archivos no responde). Se puede reintentar
  más tarde; la ayuda no se guarda para no perder en silencio una foto que la persona mandó (CU40).
- **Dónde se lanza:** `AyudaTableroService.crear()`
