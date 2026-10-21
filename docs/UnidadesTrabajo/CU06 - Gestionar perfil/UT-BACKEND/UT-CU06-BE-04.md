# UT-CU06-BE-04 — `ValidadorImagenPort` y revisión de la foto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-04` |
| **Nombre** | `ValidadorImagenPort` y revisión de la foto |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Lo que alguien sube como foto tiene que ser de verdad una imagen, no un archivo disfrazado.

## Objetivo

Un puerto que revisa los bytes de una foto y su adaptador, con la revisión básica definida en el CU (sin antivirus ni servicio externo).

## Alcance

- `domain/port/out/ValidadorImagenPort`: `ImagenValidada validar(byte[] contenido)`; `ImagenValidada` es un `record(FormatoImagen formato, int ancho, int alto)` y `FormatoImagen` un enum `JPEG`, `PNG`, `WEBP` con `extension()` y `contentType()`. Lanza `ImagenInvalidaException` (de `domain`, sin Spring) cuando se rechaza, con un motivo (`FORMATO`, `ILEGIBLE`, `DATOS_EXTRA`, `DIMENSIONES`).
- `infrastructure/adapter/out/imagen/ImagenJdkValidadorAdapter`, en este orden:
  1. **Formato por contenido:** firma de bytes (`FF D8 FF` JPEG, `89 50 4E 47 0D 0A 1A 0A` PNG, `RIFF....WEBP` WebP). Cualquier otra → `FORMATO`. No se mira la extensión ni el `Content-Type`.
  2. **Imagen completa:** `ImageIO` lee el encabezado y se comprueban las dimensiones (`app.perfil.foto.max-lado-px=4000`) **antes** de decodificar los píxeles (evita imágenes bomba) → `DIMENSIONES`; después decodifica de punta a punta → si falla, `ILEGIBLE`.
  3. **Sin bytes después del final:** JPEG termina en el marcador `FF D9` (se toleran ceros de relleno); PNG termina exactamente en el chunk `IEND` + su CRC; WebP: el tamaño del contenedor RIFF + 8 es igual al largo del archivo. Si sobra algo → `DATOS_EXTRA`.
- `pom.xml`: `com.twelvemonkeys.imageio:imageio-webp` (y su `imageio-core`), versión fijada, para decodificar WebP con `ImageIO`.
- Sin escribir nada en disco: se trabaja sobre los bytes en memoria. Nunca se loguea el contenido; en los rechazos, `WARN` con `module="perfil"` y el motivo.
- Tests (recursos de prueba pequeños generados en el test, no archivos reales grandes): imagen JPEG, PNG y WebP válidas → formato y dimensiones correctos; un `.exe` o un texto renombrado → `FORMATO`; PNG truncado → `ILEGIBLE`; PNG/JPEG con bytes pegados al final → `DATOS_EXTRA`; imagen de 4001×1 → `DIMENSIONES`; arreglo vacío → `FORMATO`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/pom.xml`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/ValidadorImagenPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/ImagenValidada.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/FormatoImagen.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/ImagenInvalidaException.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/imagen/ImagenJdkValidadorAdapter.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/imagen/ImagenJdkValidadorAdapterTest.java`

## Fuera de alcance (no tocar)

- Comprobar el tamaño en bytes (5 MB): lo corta Spring al recibir el multipart (UT 12) y el service lo vuelve a comprobar (UT 07).
- Convertir `ImagenInvalidaException` en un código de error (UT 07).
- Un antivirus, listas de firmas de malware o llamadas a servicios externos.

## Resultado esperado

Un archivo que no sea una imagen JPG, PNG o WebP completa, legible, de hasta 4000×4000 px y sin datos extra, no pasa la revisión.

## Validación

1. `./mvnw test` en `identidad` pasa con `ImagenJdkValidadorAdapterTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
