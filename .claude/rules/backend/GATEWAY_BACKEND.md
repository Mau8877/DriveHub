---
globs: projects/backend/gateway/**/*
---

# API Gateway

`projects/backend/gateway` (paquete `com.drivehub.gateway`) es el **único
punto de entrada** a la API: el navegador y la app móvil solo hablan con
él (puerto 8080, o `/api` detrás de nginx). No tiene base de datos ni
lógica de negocio.

## Stack y versión

- **Spring Cloud Gateway, variante Server Web MVC** (servlet), no la
  reactiva: permite reusar el `RateLimitFilter` servlet y el mismo modelo
  de programación que el resto de los servicios Spring.
- **Spring Boot 4.0.x + Spring Cloud 2025.1.x**, no Boot 4.1 como los
  demás servicios: el tren de Spring Cloud que soporta Boot 4.1 (2026.0)
  todavía no tiene release GA. Se sube a 4.1 cuando salga, no antes
  (Spring Cloud valida la versión de Boot al arrancar).

## Qué hace (y qué no)

| Responsabilidad | Dónde |
| --- | --- |
| Enrutar `/api/v1/{recurso}/**` al servicio dueño | `application.properties` (`spring.cloud.gateway.server.webmvc.routes[*]`) |
| CORS de toda la API | `config/CorsConfig.java` (filtro servlet con máxima prioridad) |
| Rate limit por IP sobre `/api/v1/**` | `security/RateLimitFilter.java` (Bucket4j, ver [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md#rate-limit)) |
| Validar el JWT (primera barrera) | `security/SecurityConfig.java`: toda la API exige token válido, salvo lo público (login, refresh, logout, verificar y recuperar correo/contraseña, el registro, `GET /api/v1/escuelas/{slug}` y `GET /api/v1/pagos/retorno`, la página fija a la que vuelve el estudiante de la pasarela de pago) |

Los microservicios **no** configuran CORS ni rate limit: el navegador
nunca les habla directo.

## Rutas

- Se enruta **por recurso** (`Path=/api/v1/vehiculos/**,...`), nunca por
  nombre de servicio (`/api/v1/escuela/...`): el contrato HTTP no expone
  cómo está partido el backend.
- La lista de recursos de cada servicio está en
  [MICROSERVICIOS.md](../MICROSERVICIOS.md#recursos-http-de-cada-servicio):
  un recurso nuevo se agrega en el mismo cambio al predicado de su servicio
  y a esa tabla.
- La URL de cada servicio se toma de una variable de entorno
  (`IDENTIDAD_URL`, `ESCUELA_URL`, ...) con default a `localhost:{puerto}`
  para correrlo fuera de Docker.
- El gateway no reescribe la ruta: el servicio recibe el mismo
  `/api/v1/...` que pidió el cliente.

## Tests

`GatewayRoutingTest` levanta el gateway real contra un servicio falso
(`HttpServer` del JDK) y verifica enrutamiento, 404 de rutas desconocidas
y preflight CORS. Una ruta nueva se cubre agregando un caso ahí si tiene
algo particular (un predicado distinto, un filtro); los paths simples no
necesitan un test por recurso.
