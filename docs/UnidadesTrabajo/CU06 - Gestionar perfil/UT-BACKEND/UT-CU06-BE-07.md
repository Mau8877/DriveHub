# UT-CU06-BE-07 — Subir y quitar la foto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-07` |
| **Nombre** | Subir y quitar la foto |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-03, UT-CU06-BE-04, UT-CU06-BE-06 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La persona sube su foto, la cambia o la quita, y solo quedan guardadas las imágenes que pasaron la revisión.

## Objetivo

`ActualizarFotoPerfilUseCase` y `QuitarFotoPerfilUseCase` con sus servicios y los códigos `ERR_IDE_16` y `ERR_IDE_17`.

## Alcance

- `IdentidadErrorCodes.FOTO_INVALIDA = ERR_IDE_16` y `FOTO_DEMASIADO_GRANDE = ERR_IDE_17` (ambos 400, `ValidationException`).
- `domain/port/in/ActualizarFotoPerfilUseCase`: `PerfilUsuario actualizar(UUID usuarioCodigo, byte[] contenido)`; `QuitarFotoPerfilUseCase`: `PerfilUsuario quitar(UUID usuarioCodigo)`.
- `application/service/ActualizarFotoPerfilService`:
  1. si `contenido.length` > `app.perfil.foto.max-bytes` (5 242 880) → `ERR_IDE_17`; vacío → `ERR_IDE_16`;
  2. `ValidadorImagenPort.validar` — un `ImagenInvalidaException` → `ERR_IDE_16` con un mensaje en español por motivo ("El archivo no es una imagen JPG, PNG o WebP", "La imagen está dañada o no se puede leer", "La imagen es demasiado grande en píxeles: máximo 4000×4000");
  3. clave nueva `usuarios/{usuarioCodigo}/{UuidV7.generar()}.{extension del formato validado}` (la extensión sale del formato real, no del nombre del archivo);
  4. `AlmacenFotosPort.guardar` con el `contentType` del formato validado;
  5. actualizar `usuario.foto_url` con la clave; si la base falla, `AlmacenFotosPort.borrar(claveNueva)` y se relanza;
  6. borrar la clave anterior (si había); si falla, `WARN` y se sigue.
  Devuelve el perfil con `PerfilArmador`.
- `application/service/QuitarFotoPerfilService`: si hay foto, pone `foto_url = null` y borra el objeto (si borrar falla, `WARN`); sin foto no hace nada y no es error. Devuelve el perfil.
- Logs: `INFO` con `module="perfil"` y `usuarioCodigo` al subir y al quitar; `WARN` con `errorCode` en los rechazos. Nunca los bytes del archivo ni la URL prefirmada.
- Documentar `ERR_IDE_16` y `ERR_IDE_17` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice en `docs/errors/README.md`.
- Tests unitarios: foto válida guarda con la clave esperada, actualiza la cuenta y borra la anterior; más de 5 MB → `ERR_IDE_17` sin tocar MinIO; imagen rechazada → `ERR_IDE_16` sin guardar; si la base falla se borra el objeto nuevo; si borrar el anterior falla igual responde bien; quitar sin foto no falla; quitar borra el objeto.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ActualizarFotoPerfilUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/QuitarFotoPerfilUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ActualizarFotoPerfilService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/QuitarFotoPerfilService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/ActualizarFotoPerfilServiceTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/QuitarFotoPerfilServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint y el multipart (UT 12) y el límite de Spring para el tamaño de la petición.
- Miniaturas, recortes o conversión de formato.
- Un job que limpie huérfanos del bucket.

## Resultado esperado

Subir una foto válida la guarda en MinIO y reemplaza la anterior; una inválida se rechaza sin dejar rastro; quitarla la borra.

## Validación

1. `./mvnw test` en `identidad` pasa con ambos tests en verde.
2. `ERR_IDE_16` (400) y `ERR_IDE_17` (400) documentados.
3. `./mvnw checkstyle:check` sin errores.
