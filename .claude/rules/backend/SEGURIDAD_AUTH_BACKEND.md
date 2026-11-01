---
globs: projects/backend/**/*
---

# Seguridad y Autenticación — Backend

## Quién hace qué

| Pieza | Dónde |
| --- | --- |
| Login, refresh, logout, emisión del JWT, usuarios, roles, permisos | Microservicio **Identidad** (CU01 a CU06) |
| CORS y rate limit | **Gateway** (ver [GATEWAY_BACKEND.md](GATEWAY_BACKEND.md)) |
| Validar el JWT en cada request | Gateway (primera barrera) **y** cada servicio (resource server: no confía en que la request venga del gateway) |
| Autorizar por rol | Cada servicio, sobre sus propios casos de uso |

## Estado actual (punto de partida)

Ningún caso de uso de autenticación está implementado. Cada servicio
Spring tiene un `SecurityConfig` que deja públicos solo `/health`,
`/prometheus` y Swagger, y **cierra todo lo demás** (usuario en memoria
autogenerado por Spring Boot, solo placeholder). Lo de abajo es el diseño
a implementar con CU02; no es opcional.

Servicios no Spring que ya validan el JWT (RS256 contra el JWKS de
Identidad, emisor `drivehub-identidad`, sin confiar en el gateway):
**Contenido** (Laravel, middlewares `jwt` y `rol:` en `app/Common/Security/`)
y **Asistente IA** (FastAPI, dependencias `usuario_actual` y
`exigir_rol("ESTUDIANTE")` en `app/common/security/`). Notificaciones
(NestJS) todavía no valida nada.

## Flujo: JWT + refresh token

> **Decisión de diseño: cookie `HttpOnly` para el refresh token, access
> token en memoria — no "todo en cookies".** Se evaluó mover también el
> access token a una cookie `HttpOnly` y se **confirmó el híbrido** (patrón
> estándar para SPAs, mismo enfoque que recomiendan Auth0/Okta):
> 1. **Inmunidad a CSRF en los endpoints de negocio**: el header
>    `Authorization` nunca se auto-envía (una cookie sí), así que no hace
>    falta un token anti-CSRF en cada endpoint.
> 2. **El refresh token (el de larga vida) queda fuera del alcance de
>    JavaScript** vía `HttpOnly`; el access token es de vida corta y nunca
>    toca disco, lo que acota el daño de un XSS a esa ventana.
> No se reabre esta decisión sin una razón concreta nueva.

- **Access token**: JWT de corta duración (~15 min), en
  `Authorization: Bearer <token>`. Claims: `sub` = `codigo_usuario` (UUID
  público de la persona), lista de roles activos, y nada sensible más.
  Viaja en el **body** de la respuesta de login; la web lo guarda en
  memoria del **shell** (nunca `localStorage`), los micro-frontends remotos
  se lo piden al shell (ver [ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md)).
- **Refresh token**: opaco (aleatorio, no JWT), larga duración (~7 días),
  guardado en la tabla **`refresh_token`** de Identidad (diseño del PDF):
  - Solo el **hash SHA-256** (`token_hash`); el valor en claro lo tiene
    únicamente el cliente.
  - `dispositivo` (`WEB` | `MOVIL`), `expira_en`, `revocado`.
  - Logout (CU03) lo marca `revocado = true`; refresh valida que exista,
    no esté revocado ni vencido. Un job borra los vencidos.
  - **Login único**: al iniciar sesión se revocan las sesiones previas del
    usuario según la política que defina CU02 (por dispositivo o todas).
- **Transporte del refresh token en la web: cookie `HttpOnly`**:

  | Cookie | `refreshToken` |
  | --- | --- |
  | Atributos | `HttpOnly`; `SameSite=Lax`; `Path=/api/v1/auth`; `Max-Age` = duración del refresh token |
  | `Secure` | `true` en producción; `false` en `dev` (HTTP local) — `app.cookies.secure` |
  | La setea | `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh` si rota |
  | La borra | `POST /api/v1/auth/logout` (`Max-Age=0`) |
  | La lee | refresh y logout, con `@CookieValue`, nunca del body |

  `SameSite=Lax` alcanza porque web y API son **same-site** (mismo origen
  detrás de nginx; en desarrollo, `localhost` con puertos distintos). El
  gateway tiene `allowCredentials(true)` en CORS. La **app móvil** no usa
  cookies: recibe el refresh token en el body y lo guarda en
  `flutter_secure_storage`.
- **Endpoints** (Identidad, `/api/v1/auth/...`): `login`, `refresh`,
  `logout`, y `recuperar`/`restablecer` contraseña (CU04, tabla
  `token_recuperacion`: hash, vigencia corta, un solo uso).
- El access token **no** se invalida antes de tiempo (stateless, por eso
  dura poco). No hay blacklist de access tokens salvo requisito concreto.

## Usuarios, roles y permisos (Identidad)

Modelo del diseño (`docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf`,
sección 1), ya creado en la migración de Identidad:

- **`usuario`**: `email` (login, único), `password_hash` (BCrypt o Argon2,
  nunca texto plano), `estado` (`ACTIVO` | `INACTIVO` | `BLOQUEADO`),
  `intentos_fallidos`, `ultimo_login`.
- **Bloqueo por intentos fallidos**: `intentos_fallidos` sube con cada
  login fallido y se reinicia con uno correcto; al superar el límite el
  `estado` pasa a `BLOQUEADO` y el login responde `423` (`LockedException`,
  ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)). La política de
  desbloqueo (automático por tiempo o manual por un admin) se define con CU02.
- **`rol`** (catálogo: `ESTUDIANTE`, `INSTRUCTOR`, `ADMIN`, sembrados en la
  migración), **`permiso`** (permisos finos, `clave` tipo `reserva.crear`),
  **`usuario_rol`** y **`rol_permiso`** (asociaciones con su propio codigo,
  no `@ManyToMany`).
- Los nombres de rol son **datos**, no un enum Java cerrado: se usan como
  constantes de texto (`RolNombres`) y son administrables sin desplegar.
- Spring Security: `UserDetailsService` de Identidad busca por `email`,
  arma `authorities` como `ROLE_{nombre}` por rol y `PERM_{clave}` por
  permiso, y `enabled = (estado == ACTIVO)`.

## Autorización en cada servicio

- El JWT lleva los roles como claim; cada servicio arma el principal desde
  el token (sin llamar a Identidad en cada request).
- Spring: `@PreAuthorize("hasRole('ADMIN')")` en el método de service /
  caso de uso, nunca en el controller.
- NestJS: guard propio por rol; FastAPI: dependencia; Laravel: middleware
  o policy. Mismo criterio: la decisión vive en la capa de negocio o en
  un guard declarativo, no repartida en `if` dentro de los controllers.
- **Usuario autenticado en un controller**: nunca se parsea el header a
  mano. En Spring, una anotación `@CurrentUserId UUID` (un
  `HandlerMethodArgumentResolver` por servicio, en su paquete de
  seguridad) que devuelve el `sub` del token = `codigo_usuario`.

## Rate limit

**Implementado en el gateway** (`gateway/security/RateLimitFilter.java`):
un único límite por IP para toda la API, los servicios no lo repiten.

- **Alcance**: solo `/api/v1/**` (`shouldNotFilter` excluye `/health` y
  `/prometheus`, que Prometheus scrapea cada 5s).
- **Corre antes de enrutar**: una IP consume cupo aunque la URL no exista
  todavía en ningún servicio.
- **Algoritmo**: token bucket con **Bucket4j** (`com.bucket4j:bucket4j_jdk11-core`),
  un bucket por IP en memoria, refill "greedy".
- **Config**: `app.ratelimit.capacity` y `app.ratelimit.refill-per-minute`
  (default 100/100) → `RATE_LIMIT_CAPACITY` / `RATE_LIMIT_REFILL_PER_MINUTE`.
- **Respuesta**: `429` con el sobre estándar y código `ERR_SYS_02`, escrito
  a mano (el filtro corre antes de cualquier handler de Spring MVC).
- **IP del cliente**: `request.getRemoteAddr()`; alcanza porque nginx es el
  único proxy delante. Con más proxies, `X-Forwarded-For` (el primer valor).

> **Limitación conocida, aceptada:** el bucket vive en memoria del
> gateway. Con más de una réplica cada una tendría su propio límite; para
> eso existe `bucket4j_jdk11-redis` (habría que sumar Redis al stack). No se
> implementa hasta que haya más de una instancia real.

## Endpoints públicos

Solo `/health`, `/prometheus`, Swagger/OpenAPI de cada servicio, y en
Identidad `POST /api/v1/auth/login`, `/refresh`, la recuperación de
contraseña (CU04) y `POST /api/v1/usuarios` si el registro (CU01) es
abierto. En Pagos, `GET /api/v1/pagos/retorno` (la página fija a la que vuelve
el estudiante de la pasarela de pago, CU26; no lee ni muestra datos) y, con
CU27, `POST /api/v1/webhooks-stripe`, el aviso de la pasarela (no lleva JWT: se autentica por su firma
`Stripe-Signature`, y sin firma válida responde `400 ERR_PAG_08`). Cualquier otro endpoint es privado por default: hacerlo público es
una decisión explícita, no un olvido.
