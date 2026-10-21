# UT-CU06-BE-03 — `AlmacenFotosPort` y adaptador MinIO

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-03` |
| **Nombre** | `AlmacenFotosPort` y adaptador MinIO |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md)

## Solicitud de negocio

Las fotos de perfil tienen que guardarse, borrarse y mostrarse desde el almacenamiento de archivos de la plataforma, sin que el dominio conozca MinIO.

## Objetivo

Un puerto de salida con tres operaciones y su adaptador contra MinIO, listos para usar desde los servicios.

## Alcance

- `domain/port/out/AlmacenFotosPort`:
  - `void guardar(String clave, byte[] contenido, String contentType)`
  - `void borrar(String clave)` (idempotente: borrar una clave inexistente no es error)
  - `String urlDeLectura(String clave)` (URL prefirmada; la TTL sale de configuración)
- `pom.xml`: BOM `software.amazon.awssdk:bom` y dependencias `software.amazon.awssdk:s3` y `software.amazon.awssdk:url-connection-client`, con versión fijada (propiedad, como `testcontainers.version`); `org.testcontainers:minio` en `test` si se usa en el test.
- `infrastructure/adapter/out/storage/MinioFotosAdapter` (con `@Component`): `S3Client` con endpoint `app.minio.url` y `S3Presigner` con endpoint `app.minio.public-url`, ambos con `forcePathStyle(true)`, región `us-east-1` y credenciales `app.minio.access-key` / `secret-key`. Un fallo de MinIO se propaga como excepción de infraestructura (no se traga): el service decide.
- `infrastructure/config/AlmacenamientoConfig` con los beans `S3Client` y `S3Presigner`.
- `application.properties`: `app.minio.public-url=${MINIO_PUBLIC_URL:http://localhost:9000}` y `app.perfil.foto.url-ttl-minutos=60`.
- Logs: `ERROR` con `module="perfil"` si MinIO falla; nunca credenciales ni la URL prefirmada completa.
- Tests: con Testcontainers (patrón `assumeTrue` de Docker): `guardar` y luego la URL prefirmada descarga el mismo contenido; `borrar` la deja de servir; borrar dos veces no falla; la URL firmada usa el host público y no el interno. Test unitario de la clave/TTL si hay lógica propia.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/pom.xml`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/AlmacenFotosPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/storage/MinioFotosAdapter.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/AlmacenamientoConfig.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/storage/MinioFotosAdapterTest.java`

## Fuera de alcance (no tocar)

- Armar la clave del objeto y decidir cuándo guardar o borrar (UT 07).
- Validar la imagen (UT 04) y los endpoints.
- Crear el bucket (ya lo crea `minio-init`) ni cambiar su política.

## Resultado esperado

El servicio puede guardar, borrar y firmar la lectura de un archivo en `drivehub-identidad` a través de un puerto.

## Validación

1. `./mvnw test` en `identidad` pasa con `MinioFotosAdapterTest` en verde (o `SKIPPED` sin Docker).
2. Manual: con `docker compose up`, guardar un archivo de prueba desde un test o jshell y abrir la URL devuelta en el navegador.
3. `./mvnw checkstyle:check` sin errores.
