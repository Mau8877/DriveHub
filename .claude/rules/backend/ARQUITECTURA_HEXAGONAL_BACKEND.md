---
globs: projects/backend/identidad/**/*
---

# Arquitectura Hexagonal — Identidad

El microservicio **Identidad** (`projects/backend/identidad`, paquete
`com.drivehub.identidad`) usa **arquitectura hexagonal** (puertos y
adaptadores). Es el único servicio con esta arquitectura: lo demás de
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md) (capas por feature)
**no** aplica acá, pero sí todo el contrato transversal (respuestas,
excepciones, logging, endpoints, identificadores).

## Idea central

El **dominio** (las reglas de cuentas, roles, sesiones) no depende de
nada: ni de Spring, ni de JPA, ni de HTTP. Se comunica con el mundo por
**puertos** (interfaces que define el propio dominio) y el mundo se
conecta por **adaptadores** (implementaciones de esos puertos):

```
            adaptadores de ENTRADA                  adaptadores de SALIDA
   (HTTP, eventos entrantes)                        (PostgreSQL, RabbitMQ, storage...)
            │                                                  ▲
            ▼                                                  │
   ┌──────────────── port/in ────────── DOMINIO ───────── port/out ────────┐
   │  casos de uso (interfaces)     modelo + reglas      repos/gateways    │
   └────────────────────────────── application/service ───────────────────┘
                         implementa port/in, usa port/out
```

## Estructura de paquetes

```
com.drivehub.identidad
├── common/                         # Núcleo compartido: excepciones, ApiResponse, UuidV7
├── domain/
│   ├── model/                      # Modelo de dominio puro (Usuario, Rol, Permiso, RefreshToken...)
│   └── port/
│       ├── in/                     # Casos de uso como interfaces (RegistrarUsuarioUseCase...)
│       └── out/                    # Lo que el dominio necesita de afuera (UsuarioRepositoryPort...)
├── application/
│   └── service/                    # Implementa los port/in usando los port/out
└── infrastructure/
    ├── adapter/
    │   ├── in/web/                 # Controllers REST: llaman a un port/in, nunca a un service concreto
    │   └── out/persistence/
    │       ├── model/              # Models JPA (@Entity, sufijo Model) + BaseModel
    │       └── repository/         # Spring Data JPA + adaptador que implementa el port/out
    └── config/                     # SecurityConfig y beans de Spring
```

Lo que ya existe: `domain/model` (Usuario, Rol, Permiso, RefreshToken,
TokenRecuperacion, EstadoUsuario, Dispositivo) y los Models JPA de las 7
tablas del diseño. Puertos, servicios, adaptadores y controllers se crean
con cada caso de uso (CU01 a CU06).

## Reglas

- **`domain/` no importa nada de `infrastructure/` ni de `application/`**,
  ni anotaciones de Spring/JPA/Jackson. Lombok (`@Getter`, `@Builder`) sí
  se permite: es solo generación de código en compilación.
  - Excepción aceptada: `domain` puede lanzar las `BusinessException` de
    `common/exception/` (llevan un `HttpStatus` de Spring). Se acepta ese
    acoplamiento mínimo para no duplicar la jerarquía de excepciones del
    resto del proyecto.
- **El modelo de dominio no tiene el id interno Long**: su identidad es
  el `codigo` (UUID). El id Long vive solo en los Models JPA.
- **Dos modelos por concepto, con nombres distintos**: `domain/model/Usuario`
  (dominio) y `infrastructure/adapter/out/persistence/model/UsuarioModel`
  (JPA, sigue la convención "Model" de
  [CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md)). El
  adaptador de persistencia los traduce con un mapper propio
  (`UsuarioPersistenceMapper`); el dominio nunca ve un `UsuarioModel`.
- **Un caso de uso = un puerto de entrada**: `RegistrarUsuarioUseCase`
  con un método (`registrar(...)`), implementado por un service de
  `application/service/`. El controller depende de la interfaz.
- **Puertos de salida con nombre de negocio**, sufijo `Port`
  (`UsuarioRepositoryPort`, `PublicadorEventosPort`), implementados por
  adaptadores con sufijo `Adapter` (`UsuarioPersistenceAdapter`).
- **Asociaciones sin entidad de dominio propia**: `usuario_rol` y
  `rol_permiso` son tablas (Models JPA `UsuarioRolModel`/`RolPermisoModel`),
  pero en el dominio se expresan como colecciones (`Usuario.roles`,
  `Rol.permisos`).
- **Transacciones** (`@Transactional`) en `application/service/`, nunca
  en el dominio ni en los controllers.
- **Logging**: en `application/service/` (equivalente al service de capas,
  ver [LOGGING_BACKEND.md](LOGGING_BACKEND.md)).

## Tests

- Dominio: tests unitarios puros (sin Spring).
- `application/service/`: unitarios con los `port/out` mockeados.
- Adaptadores web: `@WebMvcTest` con el `port/in` mockeado, verificando el
  sobre `ApiResponse` (ver [TESTING_BACKEND.md](TESTING_BACKEND.md)).
- Adaptadores de persistencia: `@DataJpaTest` / Testcontainers.
