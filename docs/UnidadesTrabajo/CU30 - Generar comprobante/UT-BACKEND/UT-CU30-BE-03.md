# UT-CU30-BE-03 — Almacenamiento de los PDF en MinIO

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-03` |
| **Nombre** | Almacenamiento de los PDF en MinIO |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Los comprobantes tienen que guardarse de forma duradera y privada, sin direcciones públicas.

## Objetivo

`AlmacenComprobantes` guarda y lee PDF en el bucket `drivehub-comprobantes` de MinIO.

## Alcance

- `features/comprobantes/almacen/AlmacenComprobantes` (interfaz): `void guardar(String clave, byte[] pdf)` y `byte[] leer(String clave)` (clave inexistente → `NoSuchElementException`). `MinioAlmacenComprobantes` la implementa con el cliente S3 (AWS SDK v2) o el de MinIO; versión fijada.
- Propiedades `app.pagos.almacen.*` (variables `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET_COMPROBANTES` por defecto `drivehub-comprobantes`), en `.env.example` y en el `environment` de `pagos` en `docker-compose.yml`. Las credenciales **nunca** van en el repo.
- Clave del objeto: `comprobantes/{escuelaCodigo}/{yyyy}/{numero}.pdf`. El bucket es **privado**: el PDF solo sale por el endpoint autenticado de la UT 06; no se generan URLs prefirmadas.
- El bucket `drivehub-comprobantes` se crea con el resto de buckets de MinIO (`infrastructure/minio/`); si no existe al guardar, el servicio lo reporta (no lo crea en silencio). [docs/INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md) lo menciona.
- Una falla de red o de MinIO se traduce a `ServicioNoDisponibleException` `ERR_PAG_15`.
- Tests: con un doble en memoria de la interfaz para el resto de las UT; `MinioAlmacenComprobantesTest` (Testcontainers MinIO con `assumeTrue(Docker)`): guarda y lee el mismo contenido byte a byte, una clave inexistente falla, el bucket inexistente se reporta; sin MinIO, el error es `ERR_PAG_15`.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/almacen/AlmacenComprobantes.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/almacen/MinioAlmacenComprobantes.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/pom.xml
- .env.example
- docker-compose.yml
- infrastructure/minio/ (creación del bucket)
- docs/INFRAESTRUCTURA.md
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/MinioAlmacenComprobantesTest.java

## Fuera de alcance (no tocar)

- Generar el PDF o decidir cuándo se guarda (UT 02 y 04).
- Direcciones prefirmadas o públicas.

## Resultado esperado

Un PDF se guarda con su clave y se recupera idéntico; el bucket no es público.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el stack levantado, un PDF de prueba aparece en el bucket `drivehub-comprobantes` de la consola de MinIO (9001).
