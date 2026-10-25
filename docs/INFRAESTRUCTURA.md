# Infraestructura

Todo lo que compone el entorno de desarrollo de DriveHub:
`docker-compose.yml`, la carpeta `infrastructure/`, redes, volúmenes y
variables de entorno. El mapa de servicios (qué hace cada uno, sus
recursos HTTP) está en
[`.claude/rules/MICROSERVICIOS.md`](../.claude/rules/MICROSERVICIOS.md).

## 1. Panorama general

```
 Navegador ──▶ proxy nginx :80 ──┬─ /api/*  ──▶ gateway :8080 ──┬─▶ identidad      :8081 ──▶ Postgres (base identidad)
                                 │                              ├─▶ escuela        :8082 ──▶ Postgres (base escuela)
                                 │                              ├─▶ pagos          :8083 ──▶ Postgres (base pagos)
                                 │                              ├─▶ academico      :8084 ──▶ MongoDB (academico)
                                 │                              ├─▶ contenido      :8085 ──▶ MongoDB (contenido)
                                 │                              ├─▶ notificaciones :8086 ──▶ DynamoDB local
                                 │                              └─▶ asistente-ia   :8087 ──▶ DynamoDB local
                                 └─ /*      ──▶ shell :4200
 El shell carga los remotos desde :4201-:4206 (directo, no por nginx).

 Todos los servicios ──▶ RabbitMQ :5672 (eventos)   ·   MinIO :9000 (archivos)
 Prometheus :9090 ──▶ scrapea /prometheus de los servicios Spring ──▶ Grafana :3000
 App móvil ──▶ gateway :8080 directo
```

Todo vive en una red Docker (`drivehub-network`) y se resuelve por nombre
de contenedor (`drivehub-gateway`, `drivehub-postgres`...). Los puertos
también se publican al host para acceder a cada pieza directo.

## 2. Perfiles: qué arranca con `docker compose up`

| Grupo | Perfil | Servicios |
| --- | --- | --- |
| Infraestructura | (ninguno: siempre arranca) | `postgres`, `mongo`, `dynamodb`, `rabbitmq`, `minio`, `minio-init`, `mailpit` |
| Backend | `backend` | `gateway`, `identidad`, `escuela`, `pagos`, `academico`, `contenido`, `notificaciones`, `asistente-ia` |
| Frontend | `frontend` | `frontend` (las 7 apps en un contenedor), `proxy` (nginx) |
| Observabilidad | `observabilidad` | `prometheus`, `grafana` |

`COMPOSE_PROFILES` en el `.env` (default `backend,frontend,observabilidad`)
decide qué perfiles se levantan. Para trabajar en un servicio desde el IDE,
dejar `COMPOSE_PROFILES=` vacío (solo infra) o sacar `backend` y correr el
servicio local; los defaults de cada servicio apuntan a `localhost`.

## 3. Puertos

| Puerto | Qué | Para qué |
| --- | --- | --- |
| `80` | nginx | Punto de entrada web: `/api` → gateway, resto → shell |
| `8080` | gateway | API completa (`/api/v1/**`), `/health`, `/prometheus` |
| `8081`–`8087` | microservicios | Acceso directo a cada servicio: `/health`, Swagger (Spring: `/swagger-ui/index.html`; FastAPI: `/docs`) |
| `4200` | shell | Dev server del shell (Native Federation) |
| `4201`–`4203` | remotos Angular | identidad, escuela, pagos |
| `4204`–`4206` | remotos React | examenes, contenido, asistente-ia |
| `5432` | PostgreSQL | Cliente SQL (cambiable con `POSTGRES_HOST_PORT`) |
| `27017` | MongoDB | Cliente Mongo (desde el host: `?directConnection=true`) |
| `8000` | DynamoDB local | `aws dynamodb ... --endpoint-url http://localhost:8000` |
| `5672` / `15672` | RabbitMQ | AMQP / consola web |
| `9000` / `9001` | MinIO | API S3 / consola web |
| `1025` / `8025` | Mailpit | SMTP de desarrollo / bandeja web de los correos |
| `9090` / `3000` | Prometheus / Grafana | Métricas |

## 4. Bases de datos

### PostgreSQL 18 (`postgres`)

Un solo contenedor con **una base y un usuario dueño por servicio**:
`identidad`, `escuela`, `pagos` (usuario = nombre de la base, password en
`IDENTIDAD_DB_PASSWORD`/`ESCUELA_DB_PASSWORD`/`PAGOS_DB_PASSWORD`). Las crea
`infrastructure/postgres/init/01-crear-bases.sh` **solo la primera vez**
que el volumen está vacío. El esquema de cada base lo crea Flyway desde su
servicio. Si se agrega una base nueva con el volumen ya creado, hay que
crearla a mano o borrar el volumen (`docker volume rm drivehub-postgres_data`).

### MongoDB 8 (`mongo`)

**Replica set de un nodo** (`rs0`): necesario para las transacciones
multi-documento (reserva + franja + outbox en Académico). El healthcheck
ejecuta `rs.initiate()` la primera vez. Sin autenticación (solo desarrollo).
Bases: `academico` y `contenido`, cada una creada por su servicio al escribir
el primer documento (índices al arrancar).

### DynamoDB local (`dynamodb`)

Imagen `amazon/dynamodb-local` con `-sharedDb` (todos los clientes ven las
mismas tablas sin importar región/credenciales). Credenciales dummy
(`AWS_ACCESS_KEY_ID=local`), que el SDK exige pero el emulador no valida.
**Cada servicio crea sus propias tablas al arrancar** si faltan
(`DYNAMODB_CREAR_TABLAS=true`): Notificaciones (`Notificaciones`,
`PreferenciasNotificacion`, `DispositivosPush`, `EventosProcesados`) y
Asistente IA (`SesionesIA`, `MensajesIA`, `AyudasTablero`, `AgentesIA`).

## 5. RabbitMQ (`rabbitmq`)

Bus de eventos entre servicios (outbox de Pagos y Académico, consumidores
en Notificaciones, etc.). Usuario/clave en `RABBITMQ_DEFAULT_USER/PASS`.
Todavía no hay exchanges ni colas: se declaran desde el código del primer
caso de uso que publique o consuma un evento.

## 6. MinIO (`minio` + `minio-init`)

Storage S3 compatible. `minio-init` (contenedor que termina al crear los
buckets) ejecuta `infrastructure/minio/create-buckets.sh`:

| Bucket | Dueño | Contenido | Acceso |
| --- | --- | --- | --- |
| `drivehub-identidad` | Identidad | Fotos de perfil | Privado (URL prefirmada) |
| `drivehub-pagos` | Pagos | PDF de comprobantes | Privado |
| `drivehub-contenido` | Contenido | Manuales (PDF) y tutoriales (video) | Lectura pública |
| `drivehub-asistente-ia` | Asistente IA | Fotos del tablero | Privado |

**`drivehub-identidad` y las URLs prefirmadas (CU06).** El bucket queda privado: la
foto de perfil se guarda con su clave (`usuarios/{codigo}/{uuid}.{ext}`) y Identidad
firma una URL de lectura (60 minutos) al responder. La firma incluye el host, así que
se hace con `MINIO_PUBLIC_URL` (el que alcanza el navegador o el teléfono), distinto de
`MINIO_URL` (`drivehub-minio:9000`, solo dentro de Docker, para escribir y borrar).

| Variable | Valor en desarrollo | Para qué |
| --- | --- | --- |
| `MINIO_URL` | `http://drivehub-minio:9000` | Subir y borrar objetos (red de Docker) |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | Firmar las URLs de lectura. Para el emulador o un teléfono real: la IP de la PC en la red (`http://192.168.1.50:9000`) |

**`drivehub-pagos` y los comprobantes (CU30).** Pagos guarda el PDF de cada comprobante
con la clave `comprobantes/{escuelaCodigo}/{yyyy}/{numero}.pdf` y nunca genera una URL de
lectura: el PDF solo sale por `GET /api/v1/comprobantes/{codigo}/pdf`, que comprueba quién
lo pide. Pagos no crea el bucket si falta: avisa (503 `ERR_PAG_15`).

| Variable (Pagos) | Valor en desarrollo | Para qué |
| --- | --- | --- |
| `MINIO_ENDPOINT` | `http://drivehub-minio:9000` | Guardar y leer los PDF (red de Docker) |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | las de `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Credenciales (nunca en el repo) |
| `MINIO_BUCKET_COMPROBANTES` | `drivehub-pagos` | El bucket privado de los comprobantes |

nginx deja pasar cuerpos de hasta 6 MB hacia `/api/` (`client_max_body_size`) para que
quepa la foto de 5 MB.

> `minio/minio` y `minio/mc` ya no existen en Docker Hub: el servidor usa la
> imagen de Chainguard (`user: '0'` para escribir en el volumen, healthcheck
> por `/dev/tcp` porque no trae curl) y el cliente la de `bitnamilegacy`.

## 7. Correo de desarrollo (`mailpit`)

[Mailpit](https://mailpit.axllent.org) es un servidor SMTP de mentira: recibe
los correos que envían los servicios y los muestra en una bandeja web, sin
entregar nada a nadie. Lo usa Identidad para recuperar la contraseña y
confirmar el correo (CU04).

| Qué | Dónde |
| --- | --- |
| SMTP (los servicios envían acá) | `drivehub-mailpit:1025` dentro de Docker, `localhost:1025` desde el host |
| Bandeja web | http://localhost:8025 |

No necesita usuario ni clave y no guarda nada entre reinicios. Para mandar
correos reales, el servicio solo cambia sus variables `SMTP_*` (host, puerto,
usuario, clave y remitente) por las de un proveedor: el código no distingue
entre uno y otro.

## 8. Servicios de backend en desarrollo

- **Spring** (`gateway`, `identidad`, `escuela`, `pagos`, `academico`):
  imagen `maven:3.9-eclipse-temurin-21-alpine` + `mvn spring-boot:run`
  (DevTools, recarga en caliente). Montan el repo y `~/.m2` del host
  (comparten caché de dependencias con `./mvnw` local). Los cuatro con base
  montan además `/var/run/docker.sock` para Testcontainers (ver
  `TESTING_BACKEND.md`).
- **Notificaciones**: `node:24-alpine` + `pnpm start:dev`; `node_modules`
  en volúmenes propios para no chocar con el `pnpm` del host (Windows).
- **Asistente IA**: imagen construida desde su `Dockerfile` (target `dev`),
  código montado y `uvicorn --reload`.
- **Contenido**: imagen construida desde su `Dockerfile` (target `dev`,
  PHP 8.4 + ext-mongodb + Composer). Al arrancar: `composer install`,
  `php artisan migrate` (colecciones e índices) y `php artisan serve`.
  `vendor/` en un volumen propio.

### Probar pagos con Stripe (CU26 y CU27)

El pago se confirma solo con el **aviso firmado** de Stripe, nunca porque la app vuelva de la página de pago. En
local Stripe no puede llamar a tu máquina, así que la CLI de Stripe reenvía los avisos al gateway. No hace falta
instalarla: va como un contenedor opcional.

1. En el `.env`: `STRIPE_SECRET_KEY` (clave de prueba `sk_test_...` de tu cuenta de Stripe) y `stripe` en
   `COMPOSE_PROFILES` (por ejemplo `backend,frontend,extras,stripe`).
2. `docker compose up -d stripe-cli`, y `docker compose logs stripe-cli` imprime `signing secret is whsec_...`.
3. Ese valor va en `STRIPE_WEBHOOK_SECRET` del `.env` y se reinicia `pagos` (`docker compose up -d pagos`).
   El secreto de la CLI se mantiene mientras uses la misma clave, así que se copia una sola vez por máquina.
4. Pagar una compra de horas con la tarjeta de prueba `4242 4242 4242 4242` deja el pago en `CONFIRMADO` y las
   horas en `GET /api/v1/billeteras-horas/mia`.

Los avisos que Pagos entiende son `checkout.session.completed` y `checkout.session.expired` (el cobro, CU27) y
`refund.updated`, `refund.failed` y `charge.refunded` (la devolución de un reembolso o una compensación, CU31): la
CLI ya reenvía esos cinco, y el aviso del panel de Stripe en un despliegue debe suscribirse a los mismos. Sin los de
devolución, un reembolso se queda en «en proceso» porque Pagos nunca se entera de que Stripe devolvió el dinero.

Quien no trabaja con pagos no necesita nada de esto: sin el perfil `stripe` el contenedor no arranca, y sin
claves `pagos` arranca igual (responde 503 al intentar cobrar). Cada persona usa su propia cuenta de prueba de
Stripe. Las claves van solo en el `.env` (ignorado por git), nunca en el repositorio. En un despliegue el aviso se
configura en el panel de Stripe apuntando a `https://{dominio}/api/v1/webhooks-stripe`, con su propio secreto, y
no se usa la CLI.

## 9. Frontend en desarrollo

Un único contenedor `frontend` (`node:24-alpine`) instala el workspace y
corre los 7 dev servers en paralelo, cada uno en su puerto. El navegador
abre el shell (puerto 80 por nginx, o 4200 directo) y el shell trae cada
remoto desde su puerto según sus manifiestos
(`projects/frontend/shell/public/federation.manifest.json` y
`remotos-react.manifest.json`). `/api` del dev server del shell se reenvía
al gateway (`API_GATEWAY_URL`). Cada app tiene su `node_modules` en un
volumen nombrado (mismo motivo que en Notificaciones).

## 10. Variables de entorno

| Archivo | Para qué |
| --- | --- |
| `.env` (raíz, desde `.env.example`) | Lo que lee `docker-compose.yml`: perfiles, credenciales de Postgres (por servicio), RabbitMQ, DynamoDB, MinIO, `APP_KEY` de Contenido |
| `projects/backend/{servicio}/.env.example` | Variables para correr ese servicio **fuera** de Docker (dentro, las pisa el `environment` del compose) |
| `projects/frontend/{remoto-react}/.env.example` | URL de la API cuando el remoto React corre suelto |
| `projects/mobile/.env.example` | `API_BASE_URL` (el gateway) |

Los `.env` reales no se commitean.

## 11. Volúmenes

Datos persistentes: `drivehub-postgres_data`, `drivehub-mongo_data`,
`drivehub-dynamodb_data`, `drivehub-rabbitmq_data`, `drivehub-minio_data`.
Aislamiento de dependencias: `drivehub-contenido_vendor`,
`drivehub-notificaciones-nm-*`, `drivehub-frontend-nm-*`. Para empezar de
cero: `docker compose down -v`.

## 12. Producción

Cada servicio tiene un `Dockerfile` de producción (multi-stage, sin root).
`docker-compose.yml` es **solo de desarrollo**; el despliegue (CD) está
pendiente de decidir el destino, ver
[`.claude/rules/CI_CD.md`](../.claude/rules/CI_CD.md#por-qué-cd-no-está-implementado-todavía).
