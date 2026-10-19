# Documentación

| Documento | Qué es |
| --- | --- |
| [`DriveHub/`](DriveHub/) | Diseño del producto: casos de uso por paquete, diseño de base de datos, bases de datos y micro-frontends (PDF, guía histórica), arquitectura (imagen), la [saga reserva-pago](DriveHub/SAGA_RESERVA_PAGO.md) y el [**diseño de base de datos real**](DriveHub/DISENO_BASE_DE_DATOS.md) (Markdown, fuente de verdad del esquema) |
| [`INFRAESTRUCTURA.md`](INFRAESTRUCTURA.md) | Docker Compose: servicios, puertos, bases, buckets, variables, volúmenes |
| [`errors/`](errors/README.md) | Catálogo de códigos de error de la API |
| [`CasosDeUso/`](CasosDeUso/) | Casos de uso, uno por archivo (formato en `.claude/rules/CASOS_DE_USO.md`) |
| [`UnidadesTrabajo/`](UnidadesTrabajo/) | Unidades de trabajo por caso de uso (formato en `.claude/rules/UNIDADES_DE_TRABAJO.md`) |

## Puesta en marcha

### Requisitos

- Docker Desktop (con "Allow the default Docker socket to be used" si se
  quieren correr los tests con Testcontainers dentro de los contenedores).
- Para trabajar fuera de Docker, según lo que se toque: JDK 21, Node 24 +
  pnpm 11, Python 3.14, PHP 8.4 con `ext-mongodb` + Composer, Flutter.

### Pasos

1. `cp .env.example .env` (raíz). Revisar `COMPOSE_PROFILES` y, si ya hay un
   Postgres local en 5432, `POSTGRES_HOST_PORT`.
2. `pnpm install` en la raíz (instala husky/commitlint/lint-staged y el
   workspace de la web + Notificaciones).
3. `docker compose up`.
4. Abrir http://localhost (shell) y verificar:
   - http://localhost:8080/health → gateway `UP`.
   - http://localhost:808{1..7}/health → cada servicio `UP`.
   - Consolas: RabbitMQ http://localhost:15672, MinIO http://localhost:9001,
     Grafana http://localhost:3000.

### Checklist de lo que falta decidir

- [ ] Dueño del estado de la saga reserva-pago (ver [SAGA_RESERVA_PAGO.md](DriveHub/SAGA_RESERVA_PAGO.md)).
- [ ] Paleta de colores de marca (hoy neutra en `packages/design-tokens` y en mobile).
- [ ] Destino de despliegue (habilita CD, ver `.claude/rules/CI_CD.md`).
- [ ] Generar y commitear `projects/backend/contenido/composer.lock` con el
      primer `composer install`.
