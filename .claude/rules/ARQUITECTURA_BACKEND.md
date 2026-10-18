---
globs: projects/backend/**/*
---

# Arquitectura Backend

**Feature-based + capas**: cada módulo de negocio (`features/{modulo}/`)
es una porción vertical completa (controller → service → repository →
model), y dentro de cada módulo el código se organiza en las capas
horizontales de siempre. No es "solo capas" (que mezclaría todos los
controllers de todos los módulos en una sola carpeta `controller/` gigante
a nivel raíz) ni "solo features" (que perdería la separación clara entre
qué expone HTTP, qué es lógica de negocio, y qué es acceso a datos).

## Estructura de paquetes

Ya definida en detalle en
[CONVENCIONES_JAVA_BACKEND.md](backend/CONVENCIONES_JAVA_BACKEND.md#estructura-de-paquetes)
(nombres de clases, dónde va cada cosa) — acá se documenta el **rol de cada
capa** y cómo fluye una request a través de ellas:

```
com.example.backend
├── common/          # Compartido entre 2+ módulos (nunca "por las dudas")
├── config/          # Beans de configuración de Spring (SecurityConfig, etc.)
├── security/        # JWT, filtros, @CurrentUserId
└── features/
    └── {modulo}/
        ├── controller/  # Entrada HTTP
        ├── enums/       # Enumeraciones de negocio (si aplica)
        ├── service/     # Lógica de negocio
        ├── repository/  # Acceso a datos (Spring Data JPA)
        ├── dto/         # Contrato con el cliente (Request/Response)
        ├── model/       # Modelo de persistencia (sigue llevando @Entity de JPA)
        └── mapper/      # Model <-> DTO (si no es trivial)
```

## Flujo de una request

```
Cliente (frontend/mobile)
   │  HTTP + JSON
   ▼
Controller          -- valida (@Valid), delega a UN método de service, envuelve en ApiResponse
   │
   ▼
Service              -- lógica de negocio, transacciones, logging (INFO/WARN/ERROR), lanza BusinessException
   │
   ▼
Repository            -- JpaRepository, queries derivadas o @Query
   │
   ▼
Model   ◀──────────── Mapper (si aplica) ────────────▶ DTO de salida
   │
   ▼
PostgreSQL
```

- **Controller**: nunca tiene lógica de negocio, nunca accede a un
  `Repository` directo, nunca hace `try/catch` de excepciones de negocio.
  Ver [ENDPOINTS_BACKEND.md](backend/ENDPOINTS_BACKEND.md#controllers-delgados).
- **Service**: acá vive la lógica real. Es la única capa que loguea (ver
  [LOGGING_BACKEND.md](backend/LOGGING_BACKEND.md#dónde-se-loguea)) y la
  única que lanza excepciones de negocio catalogadas (ver
  [EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md)). Recibe/devuelve
  DTOs a la capa de arriba (el controller), pero internamente trabaja con
  Models.
- **Repository**: sin lógica, solo acceso a datos. `JpaRepository<Model, Id>`
  + queries derivadas por nombre de método o `@Query` cuando el nombre se
  vuelve inmanejable.
- **DTO**: es el contrato con el cliente, nunca se expone un `Model` (la
  clase JPA) directo en un response (evita filtrar detalles de persistencia,
  permite que el modelo de datos cambie sin romper el contrato HTTP).
- **Mapper**: solo cuando el mapeo Model↔DTO no es trivial (campos
  calculados, aplanar relaciones, etc.) — si es un mapeo 1:1 de campos, se
  hace inline en el service, no se crea una clase mapper para eso.

## Transversal a todos los módulos

Estas piezas no viven en ningún `features/{modulo}/` porque las usa **toda**
la aplicación, no un módulo puntual — cada una tiene su propia rule con el
detalle completo:

| Pieza | Vive en | Rule |
| --- | --- | --- |
| Sobre de respuesta (`ApiResponse`, `PageMeta`) | `common/response/` | [RESPONSES_BACKEND.md](backend/RESPONSES_BACKEND.md) |
| Jerarquía de excepciones + `GlobalExceptionHandler` | `common/exception/` | [EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md) |
| `BaseModel` (id interno + codigo UUID público, timestamps, soft delete) | `common/model/` | [CONVENCIONES_JAVA_BACKEND.md](backend/CONVENCIONES_JAVA_BACKEND.md) |
| Autenticación JWT, `RateLimitFilter`, `@CurrentUserId` | `security/` | [SEGURIDAD_AUTH_BACKEND.md](backend/SEGURIDAD_AUTH_BACKEND.md) |
| `SecurityConfig` y demás config de Spring | `config/` | — |
| Convención de rutas, verbos HTTP, paginación | (no es código, es contrato) | [ENDPOINTS_BACKEND.md](backend/ENDPOINTS_BACKEND.md) |
| Logging estructurado | (transversal a `service/` de cada módulo) | [LOGGING_BACKEND.md](backend/LOGGING_BACKEND.md) |
| Migraciones, nombres de tabla/columna | `src/main/resources/db/migration/` | [PERSISTENCIA_BD_BACKEND.md](backend/PERSISTENCIA_BD_BACKEND.md) |

## Regla de dependencia

`features/{modulo}/` puede depender de `common/` y `security/` — nunca al
revés (`common/`/`security/` no importan nada de `features/`, o dejarían
de ser transversales). Un módulo de `features/` **no** depende de otro
módulo de `features/` directo salvo que la relación de negocio lo
justifique explícitamente (ej. `pedidos` necesita leer `usuarios`) — y en
ese caso, a través del `service` del módulo dueño del dato, nunca accediendo
a su `repository` desde afuera.
