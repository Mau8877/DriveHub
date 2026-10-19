# DriveHub

Plataforma para autoescuelas: gestión escolar (autos, estudiantes,
docentes), clases y horarios, exámenes teóricos y prácticos, pagos con QR,
manuales y tutoriales, notificaciones y un asistente con IA (foto del
tablero, prueba de conducción asistida).

Monorepo con **8 servicios de backend**, **7 micro-frontends web** y una
**app móvil**.

## Arquitectura

```
        Web (shell Angular + remotos Angular/React)        App móvil (Flutter)
                              │                                    │
                              └──────────────┬─────────────────────┘
                                     API Gateway (Spring Cloud Gateway)
      ┌───────────┬───────────┬──────────┬──────────┬──────────────┬───────────────┐
  Identidad    Escuela      Pagos    Académico  Contenido  Notificaciones  Asistente IA
  Spring       Spring       Spring    Spring     Laravel     NestJS          FastAPI
  hexagonal    clean        capas     capas      capas       capas           capas
  PostgreSQL   PostgreSQL   Postgres  MongoDB    MongoDB     DynamoDB        DynamoDB
      └───────────┴───────────┴──── RabbitMQ (eventos) ────┴──────────────┴───────────────┘
                                   MinIO (PDF, video, fotos)
```

| Proyecto | Stack |
| --- | --- |
| `projects/backend/gateway` | Spring Cloud Gateway (Boot 4.0) |
| `projects/backend/identidad` | Java 21, Spring Boot 4.1, hexagonal, PostgreSQL + Flyway |
| `projects/backend/escuela` | Java 21, Spring Boot 4.1, clean architecture, PostgreSQL + Flyway |
| `projects/backend/pagos` | Java 21, Spring Boot 4.1, capas, PostgreSQL + Flyway |
| `projects/backend/academico` | Java 21, Spring Boot 4.1, capas, MongoDB |
| `projects/backend/contenido` | PHP 8.4, Laravel 13, capas, MongoDB |
| `projects/backend/notificaciones` | Node 24, NestJS 12, capas, DynamoDB |
| `projects/backend/asistente-ia` | Python 3.14, FastAPI, capas, DynamoDB |
| `projects/frontend/shell` | Angular 22, host de Native Federation (login, layout, rutas) |
| `projects/frontend/{identidad,escuela,pagos}` | Angular 22, remotos de Native Federation |
| `projects/frontend/{examenes,contenido,asistente-ia}` | React 19 + Vite, remotos de Module Federation |
| `packages/design-tokens`, `packages/mf-contract` | Tokens de diseño y contrato shell↔remotos |
| `projects/mobile` | Flutter, Riverpod, go_router, dio |

Mapa detallado (recursos de cada servicio, puertos, prefijos de error):
[`.claude/rules/MICROSERVICIOS.md`](.claude/rules/MICROSERVICIOS.md).

## Puesta en marcha rápida

```bash
cp .env.example .env
pnpm install
docker compose up
```

Abrir http://localhost. Detalle de cada servicio, puertos y variables en
[`docs/INFRAESTRUCTURA.md`](docs/INFRAESTRUCTURA.md); checklist completo en
[`docs/README.md`](docs/README.md).

## Estado

Estructura, arquitectura y modelo de datos de todos los servicios
implementados (Models, migraciones, índices, contrato de respuestas y
errores, health checks, logging, CI). **Ningún caso de uso implementado
todavía**: se desarrollan a partir de los Casos de Uso y Unidades
de Trabajo en `docs/`.

## Convenciones

- Reglas de arquitectura y código: [`.claude/rules/`](.claude/rules/).
- Commits: `tipo[SCOPE]: servicio -- descripción` (validado por commitlint),
  ver [`.claude/rules/CONVENCIONES_GIT.md`](.claude/rules/CONVENCIONES_GIT.md).
- Errores de la API: [`docs/errors/`](docs/errors/README.md).
