# UT-CU32-BE-02 — Almacenamiento del PDF en MinIO

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-BE-02` |
| **Nombre** | Almacenamiento del PDF en MinIO |
| **Caso de uso** | CU32-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — |

## Antes de implementar

[LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md) y `infrastructure/minio/create-buckets.sh` (el bucket `drivehub-contenido` es de lectura pública).

## Solicitud de negocio

Los manuales son PDF: tienen que guardarse en algún lado y poder abrirse desde el navegador.

## Objetivo

Un servicio que sube un PDF al bucket de la escuela y valida que un descriptor apunte a un archivo propio.

## Alcance

- `composer.json`: `league/flysystem-aws-s3-v3 ^3.0`.
- `config/filesystems.php`: disco `contenido` (S3 contra MinIO, ruta por prefijo, `public_url` para el navegador).
- `ArchivoManualService::subir()` (clave `escuelas/{escuela}/manuales/{uuid7}.pdf`, descriptor con URL pública, MIME, tamaño, SHA-256 y nombre) y `validarDescriptor()` (la URL debe estar en la carpeta de la escuela y el MIME ser PDF; si no, `ERR_CON_03`).
- `docker-compose.yml` (servicio `contenido`): `MINIO_PUBLIC_URL`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `IDENTIDAD_URL` y dependencia de `minio`. `.env.example` del servicio.

Archivos esperados (crear/tocar): `composer.json`, `composer.lock`, `config/filesystems.php`, `ArchivoManualService.php`, `docker-compose.yml`, `.env.example`.

## Fuera de alcance (no tocar)

- Borrar archivos huérfanos o reemplazados.
- Videos y archivos de tutoriales (CU33).

## Resultado esperado

Subir un PDF devuelve su descriptor y la URL responde 200 desde el navegador.

## Validación

1. Con el stack levantado, `POST /api/v1/manuales/archivos` con un PDF responde 201 y `GET` a la `url` devuelve el archivo.
2. Una `url` de otra escuela o un MIME distinto se rechazan con `ERR_CON_03`.
