---
globs: projects/backend/notificaciones/**/*
---

# NestJS — Notificaciones

El microservicio **Notificaciones** (`projects/backend/notificaciones`)
está en **NestJS 12** (Node 24, TypeScript, ESM) con arquitectura de
**capas por feature**, el mismo criterio que
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md) traducido a Nest.
Base de datos: **DynamoDB** (ver
[PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md#dynamodb)).
Es parte del workspace pnpm de la raíz.

## Estructura

```
src/
├── main.ts                 # Bootstrap: logger JSON, ValidationPipe, GlobalExceptionFilter
├── app.module.ts           # Módulo raíz: importa ConfigModule, DynamoDbModule y cada feature
├── common/
│   ├── response/           # ApiResponse / ApiResponses (sobre estándar)
│   ├── exceptions/         # BusinessException + ValidationException, NotFoundException...
│   ├── filters/            # GlobalExceptionFilter (equivale al GlobalExceptionHandler de Spring)
│   ├── dynamodb/           # Cliente, definición de tablas (DEFINICION_TABLAS) y CreadorTablasService
│   └── idempotencia/       # EventoProcesado: idempotencia de los consumidores de RabbitMQ
├── health/                 # GET /health
└── features/
    └── {feature}/          # notificaciones/, preferencias/, dispositivos/
        ├── {feature}.module.ts
        ├── controllers/    # Entrada HTTP (/api/v1/...)
        ├── services/       # Lógica de negocio, logging, lanza BusinessException
        ├── repositories/   # Acceso a DynamoDB (DocumentClient), sin lógica
        ├── models/         # Forma del ítem de DynamoDB (interface) + helpers de clave
        ├── dto/            # Request/Response con class-validator
        └── enums/
```

Hoy solo existen los `models/` y `enums/` de cada feature (ningún caso de
uso implementado): el `{feature}.module.ts` se crea con el primer
controller/service de esa feature y se importa en `app.module.ts`.

## Reglas

- **Mismas capas y responsabilidades que en Spring**: controller delgado
  (valida con `ValidationPipe` + DTO de class-validator, llama a un método
  del service, devuelve `ApiResponses.success(...)`), service con la lógica
  (única capa que loguea y lanza `BusinessException`), repository sin lógica.
- **Prefijo de rutas**: los controllers declaran la ruta completa
  (`@Controller('api/v1/notificaciones')`); `/health` queda fuera de `/api/v1`.
- **Errores**: solo `BusinessException` (y subclases) con código
  `ERR_NOT_NN`. Nunca `HttpException` de Nest desde código de negocio:
  `GlobalExceptionFilter` traduce las de Nest (validación → `ERR_SYS_01`,
  ruta inexistente → `ERR_SYS_03`, resto → `ERR_SYS_00`).
- **Logging**: `Logger` de Nest con salida JSON (configurada en
  `main.ts`), mensaje + objeto de contexto (`{ module: 'notificaciones', ... }`),
  nunca concatenando datos al mensaje.
- **Configuración**: `ConfigService` (`@nestjs/config`), nunca
  `process.env` directo fuera de `main.ts`. Variables en `.env.example`.
- **ESM**: los imports relativos llevan extensión `.js`
  (`import { X } from './x.js'`), es el modo `nodenext` que genera Nest 12.
- **UUID v7** para los codigos públicos: paquete `uuid` (`v7()`), se agrega
  como dependencia con el primer caso de uso que cree ítems.
- **Estilo**: el `.prettierrc` de la raíz (sin punto y coma, comillas
  simples). Lint: `oxlint --type-aware` (`pnpm lint`).

## Tests

**Vitest** (lo que trae Nest 12), co-ubicados: `x.service.spec.ts` al lado
de `x.service.ts`. Foco igual que [TESTING_BACKEND.md](TESTING_BACKEND.md):
services (con el repository mockeado) y controllers (contrato HTTP y forma
del sobre, con `@nestjs/testing` + supertest). Regla estricta de tests al
día: igual que en Spring.

```bash
pnpm --dir projects/backend/notificaciones test
pnpm --dir projects/backend/notificaciones lint
pnpm --dir projects/backend/notificaciones build
```
