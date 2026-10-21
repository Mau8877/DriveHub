# UT-CU06-BE-02 — Infraestructura: URL pública de MinIO y límite de subida

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-02` |
| **Nombre** | Infraestructura: URL pública de MinIO y límite de subida |
| **Caso de uso** | CU06-BE |
| **Microservicio** | infraestructura (`nginx`, `docker-compose.yml`, `.env.example`); la variable la lee `identidad` |
| **Depende de** | — |

## Antes de implementar

[INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md)

## Solicitud de negocio

La foto se sube a través del gateway y se muestra desde MinIO con una URL que el navegador o el teléfono puedan abrir; hoy nada de eso está preparado.

## Objetivo

Existe `MINIO_PUBLIC_URL` para firmar las URLs de lectura, y nginx deja pasar subidas de hasta 5 MB.

## Alcance

- `.env.example`: `MINIO_PUBLIC_URL=http://localhost:9000` con un comentario que explique que es el host que alcanza el navegador o el teléfono (no el de Docker) y que, para el emulador o un teléfono real, va la IP de la PC en la red, por ejemplo `http://192.168.1.50:9000`.
- `docker-compose.yml`: en el servicio `identidad`, `MINIO_PUBLIC_URL=${MINIO_PUBLIC_URL:-http://localhost:9000}`. No se cambian los demás servicios.
- `infrastructure/nginx/nginx.conf`: `client_max_body_size 6m;` dentro de `location /api/` (nginx corta por defecto en 1 MB; 6 MB cubre 5 MB de archivo más el envoltorio multipart).
- `docs/INFRAESTRUCTURA.md`: en la sección de MinIO, explicar `MINIO_PUBLIC_URL` y que el bucket `drivehub-identidad` queda privado y se lee con URL prefirmada.

Archivos esperados (crear/tocar):

- `.env.example`
- `docker-compose.yml`
- `infrastructure/nginx/nginx.conf`
- `docs/INFRAESTRUCTURA.md`

## Fuera de alcance (no tocar)

- El adaptador que usa la variable (UT 03) y `application.properties` de `identidad`.
- Los buckets de otros servicios y su política de acceso.
- Subir el límite de peticiones del gateway o de otros servicios.

## Resultado esperado

Con `docker compose up`, `identidad` conoce la URL pública de MinIO y nginx acepta un cuerpo de 5 MB hacia `/api/`.

## Validación

1. `docker compose config` muestra `MINIO_PUBLIC_URL` en `identidad`.
2. `docker compose exec nginx nginx -t` responde que la configuración es válida.
3. Manual: `curl -F archivo=@cinco_mb.bin http://localhost/api/v1/usuarios/me/foto` ya no responde `413` (responderá `401` hasta que existan los endpoints).
