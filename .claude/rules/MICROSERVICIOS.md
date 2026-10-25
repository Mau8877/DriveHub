# Microservicios — mapa y reglas transversales

Aplica a todo el repositorio. Es la **fuente de verdad del mapa de
servicios** de DriveHub: qué servicio existe, en qué stack y arquitectura,
con qué base de datos, en qué puerto, y qué recursos HTTP le pertenecen.
Se deriva de los documentos de diseño en
[`docs/DriveHub/`](../../docs/DriveHub/) (casos de uso por paquete, diseño
de base de datos, bases de datos y micro-frontends).

## Mapa de servicios (backend)

Todos viven en `projects/backend/{servicio}/`, cada uno es un proyecto
independiente (su propio build, sus tests, su Dockerfile, su base).

| Servicio | Stack | Arquitectura | Base de datos | Puerto | Prefijo de error | Rule de arquitectura |
| --- | --- | --- | --- | --- | --- | --- |
| `gateway` | Spring Cloud Gateway (Boot 4.0) | — (solo enruta) | — | 8080 | `SYS` | [GATEWAY_BACKEND.md](backend/GATEWAY_BACKEND.md) |
| `identidad` | Spring Boot 4.1 | Hexagonal | PostgreSQL (`identidad`) | 8081 | `IDE` | [ARQUITECTURA_HEXAGONAL_BACKEND.md](backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) |
| `escuela` | Spring Boot 4.1 | Clean | PostgreSQL (`escuela`) | 8082 | `ESC` | [ARQUITECTURA_CLEAN_BACKEND.md](backend/ARQUITECTURA_CLEAN_BACKEND.md) |
| `pagos` | Spring Boot 4.1 | Capas (feature-based) | PostgreSQL (`pagos`) | 8083 | `PAG` | [ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md) |
| `academico` | Spring Boot 4.1 | Capas (feature-based) | MongoDB (`academico`) | 8084 | `ACA` | [ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md) |
| `contenido` | Laravel 13 (PHP 8.4) | Capas (feature-based) | MongoDB (`contenido`) | 8085 | `CON` | [LARAVEL_BACKEND.md](backend/LARAVEL_BACKEND.md) |
| `notificaciones` | NestJS 12 (Node 24) | Capas (feature-based) | DynamoDB | 8086 | `NOT` | [NESTJS_BACKEND.md](backend/NESTJS_BACKEND.md) |
| `asistente-ia` | FastAPI (Python 3.14) | Capas (feature-based) | DynamoDB | 8087 | `IA` | [FASTAPI_BACKEND.md](backend/FASTAPI_BACKEND.md) |

El puerto es el mismo dentro y fuera de Docker (ver
[`docker-compose.yml`](../../docker-compose.yml)): un servicio corrido
desde el IDE y el mismo servicio en su contenedor se comportan igual.

### Recursos HTTP de cada servicio

El gateway enruta **por recurso**, no por nombre de servicio: el cliente
pide `/api/v1/vehiculos` sin saber que lo atiende Escuela. Esta tabla es
el espejo de las rutas de
[`gateway/src/main/resources/application.properties`](../../projects/backend/gateway/src/main/resources/application.properties):

| Servicio | Recursos (`/api/v1/...`) |
| --- | --- |
| `identidad` | `auth`, `usuarios`, `estudiantes`, `roles`, `permisos` |
| `escuela` | `escuelas`, `instructores`, `vehiculos`, `categorias-licencia`, `asignaciones-vehiculo`, `asignaciones-alumno` |
| `pagos` | `planes-pago`, `pagos`, `comprobantes` |
| `academico` | `plantillas-horario`, `franjas`, `solicitudes-clase`, `reservas`, `clases`, `pruebas-clase`, `examenes-conduccion`, `preguntas`, `simulacros`, `intentos-simulacro`, `resultados` |
| `contenido` | `manuales`, `tutoriales` |
| `notificaciones` | `notificaciones`, `preferencias-notificacion`, `dispositivos-push` |
| `asistente-ia` | `sesiones-ia`, `ayudas-tablero`, `agentes-ia` |

**Un recurso nuevo se agrega en el mismo cambio a las dos partes**: al
predicado `Path=` de su servicio en el gateway, y a esta tabla. Un recurso
que no está en el gateway devuelve 404 aunque el servicio lo implemente.

## Reglas transversales

### Una base por servicio, sin excepciones

Ningún servicio lee ni escribe la base de otro (ni por SQL, ni con un
usuario compartido, ni "solo para un reporte"). En desarrollo, Postgres es
un solo contenedor pero con **una base y un usuario dueño por servicio**
(ver `infrastructure/postgres/init/01-crear-bases.sh`). Si un servicio
necesita un dato de otro, lo pide por su API o lo recibe por eventos.

### Identificadores

Detalle completo en
[PERSISTENCIA_BD_BACKEND.md](backend/PERSISTENCIA_BD_BACKEND.md); el
resumen que importa entre servicios:

- Toda fila/documento tiene un **id interno Long** (nunca sale del
  servicio) y un **`codigo_{tabla}` UUID v7** público (el único que viaja
  en URLs, DTOs, JWT y eventos). DynamoDB es la excepción: no tiene id
  autoincremental, su clave ya es el codigo público.
- **Persona única**: el `codigo_usuario` de Identidad es el identificador
  de la persona en toda la plataforma. Escuela, Pagos, Académico,
  Contenido, Notificaciones y Asistente IA referencian al estudiante,
  instructor o administrador con ese mismo UUID.
- **Referencias a otro servicio**: columna/campo `{entidad}_codigo` (ej.
  `estudiante_codigo`, `vehiculo_codigo`, `reserva_codigo`) con el codigo
  público del dueño, **sin FK física**. Se valida al escribir (llamada
  REST al dueño) o se mantiene por eventos.

### Comunicación

| Quién → quién | Cómo |
| --- | --- |
| Web / móvil → backend | Siempre por el gateway (`:8080`, o `/api` detrás de nginx). Nunca directo a un servicio. |
| Servicio → servicio (consulta) | REST interno, al servicio dueño del dato, por su URL interna de Docker. |
| Servicio → servicios (algo pasó) | Evento en RabbitMQ, publicado con **outbox** (la tabla/colección `outbox_evento(s)` se escribe en la misma transacción que el cambio; un proceso aparte publica). |
| Consumidor de eventos | **Idempotente**: el mismo evento puede llegar dos veces (ver `EventosProcesados` en Notificaciones). |

Hoy no hay ningún evento implementado. Las llamadas REST entre servicios que ya
existen son:

| Desde → hacia | Para qué | Caso de uso |
| --- | --- | --- |
| Identidad → Escuela | Consultar la escuela por slug o por codigo y si está activa | CU01, CU02 |
| Escuela → Identidad | Crear al Administrador de una escuela y, si algo falla, revertirlo | CU05 |
| Identidad → Escuela | Validar que una categoría de licencia existe (reenvía el Bearer del Administrador) | CU08 |
| Escuela → Identidad | Crear, consultar, buscar, reubicar y revertir la cuenta de un instructor (reenvía el Bearer del Administrador) | CU09 |
| Pagos → Identidad | Consultar el perfil del estudiante que compra, para copiar su nombre y el de su escuela al pago (reenvía el Bearer del estudiante) | CU26 |

Las demás se agregan con el primer caso de uso que las necesite, siguiendo esta tabla.
Esas dos se llaman entre sí: es una dependencia circular aceptada a propósito, porque
cada una consulta datos que son del otro servicio.

### Saga reserva-pago (CU44)

Reservar una clase (Académico) y pagarla (Pagos) cruza dos bases: no hay
una transacción que abarque las dos. La saga coordina los pasos y, si
algo falla, **compensa** (libera la franja, revierte el pago). Diseño y
decisión pendiente (quién es el dueño del estado de la saga) en
[`docs/DriveHub/SAGA_RESERVA_PAGO.md`](../../docs/DriveHub/SAGA_RESERVA_PAGO.md).
No se implementa nada de la saga fuera de CU44.

### Contrato común, sin importar el stack

Los 7 servicios de negocio respetan exactamente el mismo contrato hacia
afuera, aunque estén en Java, TypeScript, Python o PHP:

- Sobre de respuesta: [RESPONSES_BACKEND.md](backend/RESPONSES_BACKEND.md).
- Excepciones y códigos `ERR_{MODULO}_{NUMERO}`: [EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md)
  y [GUIA_ERRORES_BACKEND.md](backend/GUIA_ERRORES_BACKEND.md). El
  `{MODULO}` es el prefijo del servicio de la tabla de arriba.
- Rutas, verbos y paginación: [ENDPOINTS_BACKEND.md](backend/ENDPOINTS_BACKEND.md).
- Logging JSON estructurado: [LOGGING_BACKEND.md](backend/LOGGING_BACKEND.md).
- `GET /health` fuera de `/api/v1`, con el formato del Actuator de Spring
  (`{ "status": "UP" | "DOWN", "components": {...} }`, 503 si está DOWN).

## Micro-frontends (web)

Detalle en [ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md). Mapa:

| App | Framework | Tipo | Puerto | Casos de uso | Rule |
| --- | --- | --- | --- | --- | --- |
| `shell` | Angular 22 | Host (Native Federation) | 4200 | CU02 login, layout, menú por rol, avisos | [ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md) |
| `identidad` | Angular 22 | Remoto (Native Federation) | 4201 | CU01, CU06 | [ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md) |
| `escuela` | Angular 22 | Remoto (Native Federation) | 4202 | CU05, CU07 a CU11 | [ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md) |
| `pagos` | Angular 22 | Remoto (Native Federation) | 4203 | CU25 | [ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md) |
| `examenes` | React 19 | Remoto (Module Federation) | 4204 | CU19, CU20, CU22, CU23 | rules de `frontend/` (React) |
| `contenido` | React 19 | Remoto (Module Federation) | 4205 | CU32 | rules de `frontend/` (React) |
| `asistente-ia` | React 19 | Remoto (Module Federation) | 4206 | CU41, CU43 | rules de `frontend/` (React) |

El móvil (`projects/mobile`, Flutter) es una sola app para estudiante e
instructor: no forma parte del esquema de micro-frontends.

## Agregar un servicio o una app nueva

No se agrega ninguno fuera de este mapa sin decidirlo explícitamente. Si
se decide, en el mismo cambio: fila en las tablas de esta rule, servicio en
`docker-compose.yml` (y su base/bucket si aplica), ruta en el gateway (o en
el manifiesto del shell), job en `.github/workflows/ci.yml`, entrada en
`lint-staged` del `package.json` raíz, y su rule de arquitectura.
