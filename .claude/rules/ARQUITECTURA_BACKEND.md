---
globs: projects/backend/**/*
---

# Arquitectura Backend

El backend son **8 proyectos independientes** en `projects/backend/` (el
gateway y 7 microservicios de negocio), cada uno con la arquitectura que
le asignó el equipo. El mapa completo (stack, base, puerto, recursos) está
en [MICROSERVICIOS.md](MICROSERVICIOS.md); acá se define qué significa cada
arquitectura y el detalle de la de **capas**.

| Arquitectura | Servicios | Rule |
| --- | --- | --- |
| Capas (feature-based) en Spring Boot | `pagos`, `academico` | Esta rule (abajo) |
| Hexagonal (puertos y adaptadores) | `identidad` | [ARQUITECTURA_HEXAGONAL_BACKEND.md](backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) |
| Clean Architecture | `escuela` | [ARQUITECTURA_CLEAN_BACKEND.md](backend/ARQUITECTURA_CLEAN_BACKEND.md) |
| Capas en NestJS | `notificaciones` | [NESTJS_BACKEND.md](backend/NESTJS_BACKEND.md) |
| Capas en FastAPI | `asistente-ia` | [FASTAPI_BACKEND.md](backend/FASTAPI_BACKEND.md) |
| Capas en Laravel | `contenido` | [LARAVEL_BACKEND.md](backend/LARAVEL_BACKEND.md) |
| Solo enrutamiento | `gateway` | [GATEWAY_BACKEND.md](backend/GATEWAY_BACKEND.md) |

**Capas** (también llamada "DDD por capas" en el equipo) es la arquitectura
por defecto: cada módulo de negocio es una porción vertical completa
(controller → service → repository → model) y adentro se organiza en las
capas horizontales de siempre. NestJS, FastAPI y Laravel aplican la misma
idea con los nombres de su framework (ver su rule). Hexagonal y Clean son
excepciones deliberadas, solo en los servicios donde se eligieron: **no se
mezclan** (no se meten puertos/adaptadores en Pagos ni carpetas `features/`
en Identidad).

## Capas en Spring Boot (`pagos`, `academico`)

### Estructura de paquetes

Detalle de nombres de clases en
[CONVENCIONES_JAVA_BACKEND.md](backend/CONVENCIONES_JAVA_BACKEND.md#estructura-de-paquetes);
acá, el rol de cada capa:

```
com.drivehub.{servicio}
├── common/          # Compartido entre 2+ módulos del servicio (nunca "por las dudas")
│   ├── exception/   # BusinessException y jerarquía + GlobalExceptionHandler
│   ├── response/    # ApiResponse, PageMeta
│   ├── model/       # BaseModel (JPA) / BaseDocument (Mongo)
│   ├── outbox/      # OutboxEvento: eventos pendientes de publicar a RabbitMQ
│   └── util/        # UuidV7
├── config/          # Beans de configuración de Spring (SecurityConfig, MongoConfig...)
└── features/
    └── {modulo}/    # ej. pagos/, inscripciones/, reservas/, preguntas/
        ├── controller/  # Entrada HTTP
        ├── enums/       # Enumeraciones de negocio (si aplica)
        ├── service/     # Lógica de negocio
        ├── repository/  # Acceso a datos (Spring Data JPA / Mongo)
        ├── dto/         # Contrato con el cliente (Request/Response)
        ├── model/       # Modelo de persistencia (@Entity JPA o @Document Mongo)
        └── mapper/      # Model <-> DTO (si no es trivial)
```

Módulos que ya existen (solo con sus Models, sin casos de uso todavía):

- **Pagos**: `planes`, `pagos`
  (pago, transacción con la pasarela, reversión), `comprobantes`.
- **Académico**: `horarios` (plantillas + franjas), `solicitudes`,
  `reservas`, `clases` (clase + prueba de clase), `examenes`, `preguntas`,
  `simulacros` (configuración + intentos), `resultados`.

### Flujo de una request

```
Cliente (frontend/mobile)
   │  HTTP + JSON  (siempre a través del gateway)
   ▼
Controller          -- valida (@Valid), delega a UN método de service, envuelve en ApiResponse
   │
   ▼
Service             -- lógica de negocio, transacciones, logging, lanza BusinessException,
   │                   escribe el outbox si el cambio tiene que avisarse a otros servicios
   ▼
Repository          -- JpaRepository / MongoRepository, queries derivadas o @Query
   │
   ▼
Model  ◀──────────── Mapper (si aplica) ────────────▶ DTO de salida
   │
   ▼
PostgreSQL / MongoDB (la base propia del servicio)
```

- **Controller**: nunca tiene lógica de negocio, nunca accede a un
  `Repository` directo, nunca hace `try/catch` de excepciones de negocio.
  Ver [ENDPOINTS_BACKEND.md](backend/ENDPOINTS_BACKEND.md#controllers-delgados).
- **Service**: acá vive la lógica real. Es la única capa que loguea y la
  única que lanza excepciones de negocio catalogadas. Recibe/devuelve DTOs
  al controller, pero internamente trabaja con Models.
- **Repository**: sin lógica, solo acceso a datos.
- **DTO**: el contrato con el cliente, nunca se expone un Model directo.
- **Mapper**: solo cuando el mapeo Model↔DTO no es trivial; un mapeo 1:1
  se hace inline en el service.

## Transversal a todos los servicios

Cada pieza tiene su propia rule con el detalle; aplican a los 7
servicios de negocio, cualquiera sea su stack:

| Pieza | Rule |
| --- | --- |
| Sobre de respuesta (`ApiResponse`, `PageMeta`) | [RESPONSES_BACKEND.md](backend/RESPONSES_BACKEND.md) |
| Jerarquía de excepciones + handler global | [EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md) |
| Identificadores, migraciones, nombres de tabla/columna/colección | [PERSISTENCIA_BD_BACKEND.md](backend/PERSISTENCIA_BD_BACKEND.md) |
| Autenticación JWT, roles | [SEGURIDAD_AUTH_BACKEND.md](backend/SEGURIDAD_AUTH_BACKEND.md) |
| Rutas, verbos HTTP, paginación | [ENDPOINTS_BACKEND.md](backend/ENDPOINTS_BACKEND.md) |
| Logging estructurado | [LOGGING_BACKEND.md](backend/LOGGING_BACKEND.md) |
| CORS y rate limit | Solo en el gateway: [GATEWAY_BACKEND.md](backend/GATEWAY_BACKEND.md) |

## Regla de dependencia

Dentro de un servicio: `features/{modulo}/` puede depender de `common/` —
nunca al revés. Un módulo no depende de otro módulo del mismo servicio
salvo que la relación de negocio lo justifique (ej. `inscripciones`
referencia el `PlanPago` de `planes`), y en ese caso a través del service
del módulo dueño, nunca de su repository.

Entre servicios: **ninguna dependencia de código**. Cada servicio tiene
su propia copia de `ApiResponse`, excepciones y `UuidV7` a propósito: un
servicio tiene que poder compilarse, testearse y desplegarse solo. No se
crea una librería compartida de backend sin decidirlo explícitamente.
