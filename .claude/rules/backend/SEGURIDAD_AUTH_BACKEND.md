---
globs: projects/backend/**/*
---

# Seguridad y Autenticación — Backend

## Estado actual (punto de partida)

`SecurityConfig.java` hoy usa el `InMemoryUserDetailsManager` autogenerado
por Spring Boot (usuario/password aleatorio impreso en el log en cada
arranque). Esto es **solo un placeholder de desarrollo** y debe
reemplazarse por la implementación real descrita abajo antes de considerar
la autenticación "hecha" — no es un requisito opcional.

## Flujo: JWT + refresh token

> **Decisión de diseño: cookie `HttpOnly` para el refresh token, access
> token en memoria — no "todo en cookies".** Se evaluó explícitamente
> mover también el access token a una cookie `HttpOnly` (para que ningún
> token sea legible por JavaScript en ningún momento) contra el esquema
> híbrido documentado abajo (access token en memoria + refresh token en
> cookie `HttpOnly`), y se **confirmó el híbrido** — es el patrón estándar
> de la industria para SPAs (mismo enfoque que recomiendan proveedores
> como Auth0/Okta), por dos razones concretas:
> 1. **Inmunidad a CSRF en los endpoints de negocio.** El header
>    `Authorization` nunca se auto-envía (a diferencia de una cookie, que
>    el browser adjunta solo) — un endpoint que exige ese header queda
>    protegido contra CSRF sin necesitar un token anti-CSRF adicional. Si
>    el access token también fuera cookie, **todos** los endpoints de
>    negocio necesitarían esa protección extra (`SameSite=Strict` no
>    alcanza solo, o un patrón de doble-submit) — complejidad real sin un
>    beneficio de seguridad proporcional para el alcance de este proyecto.
> 2. **El refresh token (el credential de larga vida, el que más importa
>    proteger) sí queda completamente fuera del alcance de JavaScript**
>    vía `HttpOnly` — el access token, al ser de vida corta (~15 min) y
>    nunca tocar disco (ni `localStorage` ni cookie), acota el daño de un
>    XSS exitoso a esa ventana de tiempo.
> No se reabre esta decisión sin una razón concreta nueva (un requisito de
> seguridad que el esquema actual no cubra) — no "por prolijidad" de tener
> todo en cookies.

- **Access token**: JWT de corta duración (ej. 15 minutos). Va en el header
  `Authorization: Bearer <token>` en cada request. Contiene el identificador
  del usuario y la lista de sus roles activos (ver
  [Roles](#roles-y-permisos)) como claims — nada sensible además de eso.
  Viaja en el **body** de la respuesta de login (el cliente lo guarda en
  memoria, nunca en `localStorage`/cookie).
- **Refresh token**: de larga duración (ej. 7 días), opaco (UUID aleatorio,
  no JWT), persistido en **Redis** (ver abajo). Se usa únicamente contra el
  endpoint de refresh, nunca contra endpoints de negocio.
- **Transporte del refresh token: cookie `HttpOnly`, no en el body.** El
  cliente (browser) nunca ve ni maneja el valor del refresh token —
  `document.cookie` no puede leerla, lo que mitiga robo por XSS. El backend
  la setea/borra explícitamente en cada respuesta relevante:

  | Cookie | `refreshToken` |
  | --- | --- |
  | Atributos | `HttpOnly`; `SameSite=Lax`; `Path=/api/v1/auth`; `Max-Age` = duración del refresh token (ej. 7 días en segundos) |
  | `Secure` | `true` en producción; `false` en `dev` (el stack local corre sobre HTTP) — controlado por `app.cookies.secure` (`application-dev.properties` lo pisa a `false`) |
  | La setea | `POST /api/v1/auth/login` (al emitir uno nuevo) y `POST /api/v1/auth/refresh` si rota el token |
  | La borra | `POST /api/v1/auth/logout` (`Max-Age=0`) |
  | La lee | `POST /api/v1/auth/refresh` y `POST /api/v1/auth/logout` — **nunca** del body del request, del header `Cookie` vía `@CookieValue` |

  `SameSite=Lax` alcanza porque frontend y backend son **same-site** (mismo
  dominio registrable `localhost` en dev vía puertos distintos, mismo
  origen real detrás de nginx en producción, ver
  [`docker-compose.yml`](../../../docker-compose.yml)) — un request
  programático (`fetch`/axios) same-site igual envía la cookie aunque sea
  `Lax`, esa restricción solo afecta navegación cross-site de nivel
  superior. El cliente HTTP (`apiClient`/`axios`) necesita
  `withCredentials: true` para que el browser adjunte/acepte esta cookie
  entre orígenes distintos (ver
  [TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)), y
  `SecurityConfig` ya tiene `allowCredentials(true)` en su configuración de
  CORS — necesario para que el browser acepte la cookie en una respuesta
  cross-origin.
- **Endpoints** (`/api/v1/auth/...`):
  - `POST /api/v1/auth/login` → valida credenciales, responde
    `{ accessToken, usuario }` en el body y setea la cookie `refreshToken`.
  - `POST /api/v1/auth/refresh` → lee el refresh token de la cookie, valida
    que no esté invalidado/expirado contra Redis, responde
    `{ accessToken }` (y opcionalmente rota la cookie).
  - `POST /api/v1/auth/logout` → lee el refresh token de la cookie, lo
    invalida en Redis, y borra la cookie en la respuesta.
- **Invalidación en logout**: el refresh token se guarda del lado del
  servidor en **Redis** con TTL igual a su vencimiento (Redis ya está en
  `docker-compose.yml`, y `spring-boot-starter-data-redis` **ya está
  agregada** en `pom.xml`, sin más wiring que la conexión automática vía
  `spring.data.redis.host`; falta el código real del storage de tokens, eso
  sigue pendiente). En logout se borra esa entrada; en refresh se valida
  contra ese storage, no solo contra el formato del token.
- El access token **no** se invalida antes de tiempo (es stateless por
  diseño) — por eso su vida útil es corta. No se implementa una blacklist de
  access tokens salvo que aparezca un requisito concreto que lo justifique.

## Reemplazo del `InMemoryUserDetailsManager`

- Model `Usuario` (tabla `users`, `features/usuarios/model/Usuario.java`,
  extiende `BaseModel` — ver [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md))
  con al menos: `username` (único), `correo`/`email` (único), `passwordHash`
  (BCrypt, nunca texto plano), `estado` (enum de negocio — ej.
  `EstadoUsuario { ACTIVO, INACTIVO, ... }`, ver
  [Roles](#roles-y-permisos) para la distinción con el bloqueo temporal
  por intentos fallidos). Todavía no existe — es un requisito de esta
  regla, no algo que ya esté implementado. El identificador público es
  `codigo` (heredado de `BaseModel`), nunca se expone el detalle interno
  de la fila.
- Login puede aceptar `username` o `correo`/`email` indistintamente en el
  mismo campo de entrada si el proyecto lo requiere — el repositorio
  resuelve contra cualquiera de las dos columnas; si no hace falta esa
  flexibilidad, un solo campo de login (`email` o `username`) alcanza.
- `UserDetailsServiceImpl implements UserDetailsService` en `security/`, que
  busca el usuario por su campo de login vía `UsuarioRepository` y arma el
  `UserDetails`/principal con sus roles activos (ver
  [Roles](#roles-y-permisos)) y su `estado` (mapeado a `enabled` de
  `UserDetails` — `enabled = true` solo si `estado == ACTIVO`, para que
  Spring Security dispare `DisabledException` automáticamente en cualquier
  otro estado).
- `SecurityConfig` pasa a usar este `UserDetailsService` + un
  `PasswordEncoder` (`BCryptPasswordEncoder`) en vez del in-memory, vía un
  `AuthenticationManager` (vía `AuthenticationConfiguration`) que el
  `AuthService` del módulo `auth` usa para validar credenciales. El filtro
  de autenticación por JWT (`OncePerRequestFilter` custom) valida el access
  token en cada request y setea el `Authentication` en el
  `SecurityContext`.

## Roles y permisos

**Modelo relacional, no un enum fijo** — un `Usuario` puede tener **varios
roles activos** a la vez, vía las entidades `Rol` (catálogo de roles, con
`nombre`, `descripcion`, `isActive`) y `UsuarioRol` (tabla de asociación
con su propio `codigo`, `isActive` y timestamps — **no** es un
`@ManyToMany` simple de JPA, porque esa fila de asociación tiene datos
propios más allá de las dos FK, así que es una entidad propia con sus dos
`@ManyToOne`). Ambas entidades viven en `features/usuarios/model/` (mismo
dueño del dato que `Usuario`). Si el alcance del proyecto no necesita
varios roles por usuario, un esquema más simple (un enum fijo de 2-3
roles, columna `role` en `Usuario`) sigue siendo válido — este modelo
relacional es el punto de partida recomendado cuando se sabe de antemano
que va a hacer falta esa flexibilidad.

- Un rol solo cuenta como activo para un usuario si tanto la fila
  `UsuarioRol.isActive` como el propio `Rol.isActive` son `true` — un rol
  desactivado globalmente (`Rol.isActive = false`) deja de otorgar acceso
  a todos los usuarios que lo tengan asignado, sin tener que tocar cada
  fila de `UsuarioRol`.
- Los nombres de rol conocidos por el sistema (ej. `"ADMIN"`, `"USUARIO"`)
  se validan/usan como constantes de texto (`RolNombres` o similar, en
  `features/usuarios/`), **no** como un enum Java cerrado — los roles son
  datos (filas de la tabla `ROL`), administrables sin desplegar código
  nuevo.
- El JWT lleva la lista de nombres de roles activos como claim (array de
  strings, ver [Flujo JWT](#flujo-jwt--refresh-token)) — `UserDetailsServiceImpl`
  arma las `authorities` como `ROLE_{nombre}` por cada rol activo.
- Se protege por rol con `@PreAuthorize("hasRole('ADMIN')")` (o
  `hasAnyRole(...)` si aplica a más de uno) a nivel de método de `service`
  (no en el controller — mantiene la regla de controllers delgados de
  [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)).
- **No confundir con el bloqueo temporal por intentos fallidos de login**:
  ese bloqueo es un estado transitorio (ej. en Redis), no un valor de
  `Usuario.estado` ni de ningún rol — son dos mecanismos independientes.

Si en algún momento un módulo necesita permisos más finos que roles
(permisos granulares por acción/recurso), se define en su propia regla
puntual — no se expande este esquema global de forma especulativa.

## Obtener el usuario autenticado en los controllers

No se extrae el JWT manualmente en cada endpoint (`request.getHeader("Authorization")`,
parsear, etc.). Se usa una anotación custom que resuelve el dato directo
desde el `SecurityContext`:

```java
@GetMapping("/{id}/pedidos")
public ResponseEntity<ApiResponse<List<PedidoResponse>>> misPedidos(@CurrentUserId UUID userId) {
    ...
}
```

`UUID`, no `Long` — es el `codigo` del usuario (el mismo valor del claim
`sub` del JWT), el identificador público que expone `BaseModel` (ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md)).

La anotación `@CurrentUserId` (y su `HandlerMethodArgumentResolver`) se
define una sola vez en `security/` — ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md) para dónde vive
exactamente el archivo, para no terminar con dos definiciones de la misma
anotación en dos lugares distintos.

## Rate limit

**Ya implementado** — `RateLimitFilter` (`security/RateLimitFilter.java`),
registrado en `SecurityConfig` con `addFilterBefore(rateLimitFilter,
UsernamePasswordAuthenticationFilter.class)` (corre antes que la
autenticación: un cliente sin token igual consume su cupo, no es gratis
intentar de más).

- **Alcance**: solo `/api/v1/**` (`shouldNotFilter` excluye todo lo demás).
  Explícitamente **no** aplica a `/health`, `/prometheus` ni
  `/swagger-ui/**`/`/v3/api-docs/**` — `/prometheus` en particular recibe
  scrape de Prometheus cada 5s (ver `infrastructure/prometheus/prometheus.yml`)
  y lo bloquearía por error si estuviera dentro del alcance.
- **Corre por prefijo de URL, no por endpoint real.** El filtro se ejecuta
  a nivel servlet, **antes** de que Spring resuelva si existe un
  `@RestController` que atienda esa ruta. Esto significa que una IP
  consume cupo igual pegándole a un endpoint real que a una URL bajo
  `/api/v1/**` que ni siquiera existe todavía (un 404) — no hace falta que
  el feature esté implementado para que el rate limit ya esté activo sobre
  su futura ruta.
- **Algoritmo**: token bucket vía **Bucket4j** (`com.bucket4j:bucket4j_jdk11-core`,
  no `bucket4j-core` — esa coordinada vieja ya no es la que resuelve Maven
  Central; se verificó contra el índice de Maven Central antes de fijar la
  versión). Un bucket por IP (`ConcurrentHashMap<String, Bucket>`), refill
  "greedy" (los tokens se regeneran continuo, no en ráfagas cada N segundos).
- **Config** (`application.properties`, sobreescribible por env var):
  `app.ratelimit.capacity` (default `100`) y
  `app.ratelimit.refill-per-minute` (default `100`) → `RATE_LIMIT_CAPACITY` /
  `RATE_LIMIT_REFILL_PER_MINUTE`.
- **Respuesta al superar el límite**: `429`, con el mismo sobre de
  [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md), código `ERR_SYS_02` (ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)). Se escribe **a mano**
  (`response.getWriter().write(...)`), no vía `ApiResponse.error(...)` ni
  `GlobalExceptionHandler` — el filtro corre a nivel servlet, antes de que
  exista un `DispatcherServlet`/`@RestControllerAdvice` que pueda intervenir.
  Igual respeta el contrato exacto del sobre para que el cliente (frontend/
  mobile) lo trate igual que cualquier otro error de la API.
- **Identificación del cliente**: `request.getRemoteAddr()`. Alcanza hoy
  porque nginx es el único proxy delante del backend dentro de la red de
  Docker. Si en el futuro hay más de un proxy en la cadena, hay que leer
  `X-Forwarded-For` con cuidado (el primer valor, no el último — un cliente
  puede falsificar ese header).

> **Limitación conocida, aceptada a propósito:** el bucket vive en memoria
> del proceso (`ConcurrentHashMap`), no es distribuido. Con una sola
> instancia del backend (el caso de este proyecto) funciona perfecto. Si en
> algún momento se corre más de una réplica, cada una tendría su propio
> límite independiente (un atacante repartiendo requests entre instancias
> efectivamente multiplica su cupo). Para eso existe
> `bucket4j_jdk11-redis` (Redis ya está en el stack y
> `spring-boot-starter-data-redis` ya está en el `pom.xml`, agregada para el
> storage de refresh tokens de más arriba) — no se implementa hasta que haya
> más de una instancia real corriendo.

> **Esto no reemplaza protección a nivel de infraestructura.** Un rate
> limit en la app (como este) es la última línea de defensa, no la única —
> nginx (que ya está en `docker-compose.yml` como proxy) también podría
> tener su propio `limit_req_zone`/`limit_req`, más barato en recursos
> porque corta el tráfico antes de que llegue a la JVM. No está configurado
> todavía; si se agrega, va en una regla de infraestructura aparte, no acá.

## Endpoints públicos

Solo quedan sin autenticación los ya definidos en `SecurityConfig`
(`/health`, `/health/**`, `/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`)
más los de `/api/v1/auth/login`, `/api/v1/auth/refresh` (obviamente, todavía
no hay token) y `/api/v1/auth/register` si el proyecto lo requiere.
Cualquier otro endpoint nuevo es privado por default — si necesita ser
público, es una decisión explícita, no un olvido de agregar la regla en
`SecurityConfig`.
