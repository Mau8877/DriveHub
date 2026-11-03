# UT-CU36-BE-08 — Infraestructura y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-08` |
| **Nombre** | Infraestructura y documentación |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-06, UT-CU36-BE-07 |

## Antes de implementar

Leer [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md).

## Solicitud de negocio

El servicio tiene que arrancar con su configuración y el resto del equipo tiene que saber cómo pedirle un aviso.

## Objetivo

Las variables, el compose y la documentación reflejan lo que CU36 construyó.

## Alcance

- `docker-compose.yml` (servicio `notificaciones`): `IDENTIDAD_URL`, `SMTP_HOST/PORT/USER/PASSWORD/FROM` (como Identidad, por defecto Mailpit), `FIREBASE_CREDENCIALES`, `NOTIFICACIONES_CREDENCIAL_SERVICIOS`, y `depends_on` de `mailpit`.
- `.env.example` (raíz) y `projects/backend/notificaciones/.env.example`: las variables nuevas, con la credencial de servicio obligatoria y `FIREBASE_CREDENCIALES` vacía (sin ella, el push queda `FALLIDA`).
- `MICROSERVICIOS.md`: el aviso por bus y la ruta interna de Notificaciones en la sección de comunicación (ya no es cierto que no haya eventos), y que `/internal/v1` no se registra en el gateway.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: nota en `Notificaciones` sobre una fila por canal y qué canal es la bandeja.
- `README.md` del servicio: contrato del mensaje `notificacion.solicitada`.

Archivos esperados (crear/tocar):

- docker-compose.yml, .env.example, projects/backend/notificaciones/.env.example, projects/backend/notificaciones/README.md
- .claude/rules/MICROSERVICIOS.md, docs/DriveHub/DISENO_BASE_DE_DATOS.md

## Fuera de alcance (no tocar)

- El gateway (no se agrega la ruta interna; las rutas públicas ya existen).
- Secretos reales: nunca van al repositorio.

## Resultado esperado

`docker compose --profile extras up notificaciones` arranca con SMTP hacia Mailpit y consumiendo la cola.

## Validación

1. `docker compose config` sin errores.
2. El servicio arranca y `GET /health` responde.
