# Infrastructure

Configuración que monta `docker-compose.yml`:

- `nginx/` — proxy del puerto 80: `/api` al gateway, el resto al shell.
- `postgres/init/` — crea una base y un usuario por servicio (identidad, escuela, pagos).
- `minio/` — crea un bucket por servicio dueño de archivos.
- `prometheus/` y `grafana/` — métricas de los servicios Spring.

Detalle completo: [`docs/INFRAESTRUCTURA.md`](../docs/INFRAESTRUCTURA.md).
