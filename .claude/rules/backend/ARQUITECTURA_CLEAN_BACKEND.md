---
globs: projects/backend/escuela/**/*
---

# Clean Architecture — Escuela

El microservicio **Escuela** (`projects/backend/escuela`, paquete
`com.drivehub.escuela`) usa **Clean Architecture**. Es el único servicio
con esta arquitectura: lo demás de
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md) (capas por feature)
**no** aplica acá, pero sí todo el contrato transversal (respuestas,
excepciones, logging, endpoints, identificadores).

## Idea central: la regla de dependencia

Círculos concéntricos; **las dependencias apuntan solo hacia adentro**.
Nada de un círculo interior sabe que existe uno exterior:

```
   ┌──────────────────────────── infrastructure ───────────────────────────┐
   │  (frameworks: Spring, JPA, PostgreSQL, RabbitMQ)                       │
   │   ┌────────────────────── presentation ───────────────────────────┐   │
   │   │  (adaptadores de interfaz: controllers REST, DTOs HTTP)        │   │
   │   │   ┌──────────────── application ──────────────────────────┐    │   │
   │   │   │  (casos de uso: orquestan entidades)                   │    │   │
   │   │   │   ┌──────────────── domain ────────────────────┐       │    │   │
   │   │   │   │  entity/ (reglas de negocio puras)          │       │    │   │
   │   │   │   │  repository/ (interfaces que el dominio pide)│      │    │   │
   │   │   │   └─────────────────────────────────────────────┘       │    │   │
   │   │   └──────────────────────────────────────────────────────────┘   │   │
   │   └──────────────────────────────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────────────────────────────┘
```

## Estructura de paquetes

```
com.drivehub.escuela
├── common/                       # Núcleo compartido: excepciones, ApiResponse, UuidV7
├── domain/
│   ├── entity/                   # Entidades de negocio puras (Estudiante, Instructor, Vehiculo...) y sus enums
│   ├── repository/               # Interfaces de repositorio que necesita el negocio (EstudianteRepository)
│   └── gateway/                  # Interfaces hacia OTROS servicios (AdministradorEscuelaGateway) y sus tipos
├── application/
│   ├── usecase/                  # Un caso de uso por clase (RegistrarVehiculoUseCase, AsignarAutoAInstructorUseCase)
│   └── dto/                      # Datos de entrada/salida de los casos de uso (no los DTO HTTP)
├── presentation/
│   └── controller/               # Controllers REST + sus Request/Response HTTP
└── infrastructure/
    ├── persistence/
    │   ├── model/                # Models JPA (@Entity, sufijo Model) + BaseModel
    │   └── repository/           # Spring Data JPA + implementación de domain/repository
    ├── client/                   # Clientes REST hacia otros servicios: implementan domain/gateway
    └── config/                   # SecurityConfig y beans de Spring
```

Lo que ya existe: `domain/entity` (CategoriaLicencia, Estudiante,
Instructor, Vehiculo, MantenimientoVehiculo, AsignacionVehiculo,
AsignacionAlumno y sus enums) y los Models JPA de las 8 tablas del diseño.
Casos de uso, implementaciones de repositorio y controllers se crean con
cada caso de uso (CU07 a CU11).

## Reglas

- **`domain/` no importa nada de `application/`, `presentation/` ni
  `infrastructure/`**, ni anotaciones de Spring/JPA/Jackson. Lombok sí.
  - Excepción aceptada: puede lanzar las `BusinessException` de
    `common/exception/` (llevan un `HttpStatus`), para no duplicar la
    jerarquía de excepciones del resto del proyecto.
- **`application/` solo conoce `domain/`**: los casos de uso reciben las
  interfaces de `domain/repository/` por constructor; nunca un
  `JpaRepository` ni un Model JPA.
- **Un caso de uso = una clase** con un método público (`ejecutar(...)`),
  nombre en infinitivo + `UseCase`. Es la clase que lleva `@Transactional`
  y la que loguea (equivalente al service de capas, ver
  [LOGGING_BACKEND.md](LOGGING_BACKEND.md)).
- **`presentation/` solo conoce `application/`**: el controller arma el
  input del caso de uso desde su Request HTTP, llama a `ejecutar(...)` y
  arma el Response + `ApiResponse`. Los Request/Response HTTP viven en
  `presentation/` y son distintos de los DTOs de `application/dto/`.
- **Dos modelos por concepto, con nombres distintos**: `domain/entity/Vehiculo`
  y `infrastructure/persistence/model/VehiculoModel` (JPA). La
  implementación del repositorio traduce entre ambos; el id interno Long
  existe solo en el Model JPA, la entidad se identifica por su `codigo`.
- **Perfiles de persona**: `Estudiante` e `Instructor` guardan el
  `usuarioCodigo` de Identidad (la persona). Es el valor con el que los
  demás servicios los referencian (ver
  [MICROSERVICIOS.md](../MICROSERVICIOS.md#identificadores)).

## Tests

- `domain/`: unitarios puros (sin Spring).
- `application/usecase/`: unitarios con los repositorios de dominio
  mockeados — es donde vive la lógica, es lo más importante de testear.
- `presentation/controller/`: `@WebMvcTest` con el caso de uso mockeado,
  verificando el sobre `ApiResponse` (ver [TESTING_BACKEND.md](TESTING_BACKEND.md)).
- `infrastructure/persistence/`: `@DataJpaTest` / Testcontainers.
