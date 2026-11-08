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
│   ├── graphql/            # GraphqlApiModule (CU45): config, límites de consulta, errores catalogados
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
        ├── graphql/        # Resolvers + tipos/inputs de GraphQL de ESTA feature (CU45); llaman a los mismos services
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

## GraphQL (CU45)

`notificaciones` expone GraphQL en `POST /api/v1/graphql` **al lado** de sus rutas REST, que no se tocan. Es código
primero (`@nestjs/graphql` + Apollo, el esquema se arma en memoria; no se escribe ningún `.gql`).

- **Un resolver por feature, en `{feature}/graphql/`**, que solo traduce entrada y salida y llama al **mismo service**
  que el controller de REST. La lógica, el logging y los `BusinessException` siguen siendo del service. Los tipos
  (`@ObjectType`) e inputs (`@InputType`/`@ArgsType`) se declaran en esa misma carpeta.
- **Mismas reglas que REST**: los inputs repiten las reglas y los mensajes de class-validator de su DTO de REST.
  Es una copia a propósito (no se decora el DTO de REST) y `reglas-iguales-a-rest.spec.ts` obliga a que coincidan: si
  se cambia una regla, se cambia en los dos y el test lo comprueba.
- **Quién es la persona sale del token** (`@UsuarioActual()`), nunca de un argumento. Ninguna consulta o mutación recibe
  el código de una persona.
- **Guards y decorador**: `JwtAuthGuard`, `RolesGuard` y `@UsuarioActual()` leen el request con `requestDe(context)`
  (`common/security/request-de.ts`), que sirve igual para un controller que para un resolver. No se usa
  `context.switchToHttp()` directo en código compartido.
- **Errores**: `GlobalExceptionFilter` devuelve un `GraphQLError` con el código catalogado en `extensions.code`, y
  `errorCatalogado` (`formatError`) garantiza que **ningún** error salga sin un `ERR_...` (los de la propia consulta,
  como sintaxis o campo inexistente, pasan a `ERR_SYS_01`; cualquier otro, a `ERR_SYS_00`). GraphQL no usa el sobre
  estándar de [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md): responde `{ data, errors }`.
- **Límites** (`limites-de-consulta.ts`): `GRAPHQL_MAX_DEPTH` (6) y `GRAPHQL_MAX_ROOT_FIELDS` (10) por operación, sin lotes
  de consultas. El gateway cuenta peticiones, no consultas, así que estos topes son los que frenan un pedido abusivo.
  Los campos de introspección (`__schema`, `__type`, `__typename`) no cuentan: la consulta estándar con que las
  herramientas exploran el esquema tiene unos 15 niveles y, sin esta excepción, la introspección de desarrollo no
  funcionaría (en producción está apagada y Apollo la rechaza antes).
- **Esquema oculto en producción**: la introspección solo se activa con `GRAPHQL_INTROSPECTION=true` (desarrollo).
- **Interruptor**: `GRAPHQL_ENABLED=false` quita el módulo y deja el servicio solo con REST. Se lee al cargar
  `AppModule`, así que se fija como variable de entorno del proceso (no en un `.env` leído después).
- **Solo se expone lo que REST ya expone**: sin listado de dispositivos y sin el token de Firebase. Un campo nuevo se
  suma a REST y a GraphQL a la vez, o se documenta por qué no.
- **Tests**: `common/graphql/graphql-api.spec.ts` (resolvers contra `POST /api/v1/graphql` con los services mockeados),
  `limites-de-consulta.spec.ts`, `reglas-iguales-a-rest.spec.ts` y los de `app.module*.spec.ts` (el interruptor).

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
