# Unidades de trabajo — CU02-BE

**Caso de uso:** Iniciar sesión y mantenerla: login con bloqueo por intentos, sesión única por dispositivo, renovación con rotación, emisión del JWT y validación del token en el gateway y los servicios Spring.
**Microservicio(s):** `identidad` (UT 01, 04 a 12, 17), `escuela` (UT 02, 03, 14), `gateway` (UT 13), `pagos` (UT 15), `academico` (UT 16) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU02 - Iniciar sesión`](../../../CasosDeUso/CU02%20-%20Iniciar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh` (Identidad, públicos), `GET /.well-known/jwks.json` (Identidad, interno) y `GET /api/v1/escuelas/por-codigo/{codigo}` (Escuela, interno)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (diseño de JWT + refresh, ya decidido), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (`identidad`), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (`escuela`), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca contraseñas ni tokens), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Token de acceso:** JWT firmado con **RS256**, 15 minutos. Claims: `iss = drivehub-identidad`, `sub` (código del usuario), `roles` (lista), `escuela` (UUID; ausente para el Super admin), `iat`, `exp`, `jti`; `kid` en el encabezado. El claim `escuela` es la base del filtro por tenant que se diseña antes de CU07/CU08.
2. **Claves:** Identidad firma con la clave privada y publica la pública en `GET /.well-known/jwks.json`. El gateway y los servicios la descargan de ahí (`jwk-set-uri`), así nadie copia claves a mano y ningún otro servicio puede emitir tokens. En `dev`, si no hay variables `JWT_PRIVATE_KEY_PEM`/`JWT_PUBLIC_KEY_PEM`, Identidad genera un par en memoria (al reiniciar, las sesiones caen); fuera de `dev` es obligatorio configurarlas.
3. **Sesión renovable:** token opaco de 32 bytes aleatorios; en la base solo el hash SHA-256 (`refresh_token`). Vigencia de 7 días contados desde cada renovación, con **rotación**: cada renovación revoca el token usado y entrega uno nuevo; un token ya usado responde 401 sin revocar nada más.
4. **Una sesión activa por dispositivo:** al iniciar sesión se revocan las sesiones previas del mismo usuario y dispositivo (`WEB` o `MOVIL`); la del otro dispositivo se conserva.
5. **Entrega del refresh token:** en `WEB`, solo en la cookie `refreshToken` (`HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth`, `Max-Age` 7 días, `Secure` fuera de `dev`); en `MOVIL`, en el cuerpo de la respuesta. El cliente declara su `dispositivo` en la petición.
6. **Quién entra por cada plataforma:** `WEB` → `SUPER_ADMIN` y `ADMIN_ESCUELA`; `MOVIL` → `ESTUDIANTE` e `INSTRUCTOR` (con varios roles basta uno permitido). Regla pura en el dominio (`AccesoPorPlataforma`).
7. **Bloqueo:** 5 intentos fallidos consecutivos bloquean la cuenta 15 minutos (`usuario.bloqueado_hasta`, migración nueva); el 5.º fallo ya responde 423; pasado el tiempo, la cuenta se desbloquea sola en el siguiente intento. Los intentos solo se cuentan para correos que existen. El login es `@Transactional(noRollbackFor = BusinessException.class)` para que el contador sobreviva a la excepción.
8. **Orden de las comprobaciones** (para no revelar estados antes de tiempo): correo inexistente → 401; cuenta bloqueada → 423; contraseña incorrecta → 401 (o 423 al llegar al 5.º); solo con la contraseña correcta se informa cuenta inactiva (403), escuela suspendida (403) o plataforma no permitida (403).
9. **Escuela suspendida:** Identidad pregunta a Escuela (`GET /api/v1/escuelas/por-codigo/{codigo}`, red interna) al iniciar y al renovar. Si Escuela no responde, el inicio de sesión de cuentas con escuela falla (500): no se puede confirmar. El Super admin no depende de Escuela. Queda listo para probar cuando exista el CU de gestionar escuelas (hoy se prueba cambiando `escuela.estado` en la base).
10. **Dónde se valida el token:** gateway (primera barrera) y los 4 servicios Spring (`identidad`, `escuela`, `pagos`, `academico`); NestJS, FastAPI y Laravel lo suman con su primer endpoint. Sin token o inválido: `401 ERR_SYS_04`; sin permiso: `403 ERR_SYS_05`.
11. **Rutas públicas del gateway:** `/health`, `/prometheus`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/usuarios` y `GET /api/v1/escuelas/{slug}`. Las llamadas entre servicios (Identidad → Escuela, servicios → JWKS) van directo por la red interna, sin gateway.

## Contrato de datos / recurso HTTP

### `POST /api/v1/auth/login` (Identidad, público)

| Campo | Tipo | Regla |
|-------|------|-------|
| `email` | string | obligatorio, formato de correo |
| `password` | string | obligatoria |
| `dispositivo` | `WEB` \| `MOVIL` | obligatorio |

| Respuesta | `data` / efecto |
|-----------|-----------------|
| `200` | `{ accessToken, expiraEnSegundos, usuario: { codigo, nombres, apellidos, roles, escuelaCodigo } }` y, **solo `MOVIL`**, `refreshToken`. En `WEB`, `Set-Cookie: refreshToken` |
| `400` | `ERR_SYS_01` (campos) |
| `401` | `ERR_IDE_04` credenciales incorrectas |
| `403` | `ERR_IDE_06` cuenta inactiva · `ERR_IDE_07` escuela suspendida · `ERR_IDE_08` plataforma no permitida |
| `423` | `ERR_IDE_05` cuenta bloqueada |
| `429` | `ERR_SYS_02` (gateway) |

### `POST /api/v1/auth/refresh` (Identidad, público)

Cuerpo `{ dispositivo, refreshToken? }` (en `WEB`, el token sale de la cookie). Misma respuesta que el login. `401 ERR_IDE_09` si la sesión no sirve (inexistente, vencida, revocada, ya usada o de otro dispositivo); los rechazos de cuenta/escuela/plataforma usan `ERR_IDE_05` a `08`.

### Códigos de error nuevos

| Código | Servicio | HTTP | Excepción |
|--------|----------|------|-----------|
| `ERR_ESC_02` | Escuela | 404 | `NotFoundException` |
| `ERR_IDE_04` | Identidad | 401 | `UnauthorizedException` |
| `ERR_IDE_05` | Identidad | 423 | `LockedException` |
| `ERR_IDE_06` | Identidad | 403 | `ForbiddenException` |
| `ERR_IDE_07` | Identidad | 403 | `ForbiddenException` |
| `ERR_IDE_08` | Identidad | 403 | `ForbiddenException` |
| `ERR_IDE_09` | Identidad | 401 | `UnauthorizedException` |
| `ERR_SYS_04` | Todos | 401 | (handler de seguridad) |
| `ERR_SYS_05` | Todos | 403 | (handler de seguridad) |

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU02-BE-01](./UT-CU02-BE-01.md) | Migración `bloqueado_hasta` y campos de dominio | `identidad` | — |
| [UT-CU02-BE-02](./UT-CU02-BE-02.md) | Consulta de escuela por código: repositorio y caso de uso | `escuela` | — |
| [UT-CU02-BE-03](./UT-CU02-BE-03.md) | Endpoint `GET /api/v1/escuelas/por-codigo/{codigo}` | `escuela` | 02 |
| [UT-CU02-BE-04](./UT-CU02-BE-04.md) | Puertos y objetos de dominio de la sesión | `identidad` | 01 |
| [UT-CU02-BE-05](./UT-CU02-BE-05.md) | `IniciarSesionService` y `ERR_IDE_04..08` | `identidad` | 04 |
| [UT-CU02-BE-06](./UT-CU02-BE-06.md) | `RenovarSesionService` y `ERR_IDE_09` | `identidad` | 04 |
| [UT-CU02-BE-07](./UT-CU02-BE-07.md) | Adaptadores de persistencia: usuarios y sesiones | `identidad` | 04 |
| [UT-CU02-BE-08](./UT-CU02-BE-08.md) | Adaptadores: JWT RS256, token opaco y BCrypt | `identidad` | 04 |
| [UT-CU02-BE-09](./UT-CU02-BE-09.md) | Escuela por código en `EscuelaRestAdapter` | `identidad` | 03, 04 |
| [UT-CU02-BE-10](./UT-CU02-BE-10.md) | `AuthController`: login, renovación y cookie | `identidad` | 04 |
| [UT-CU02-BE-11](./UT-CU02-BE-11.md) | Endpoint público JWKS | `identidad` | 08 |
| [UT-CU02-BE-12](./UT-CU02-BE-12.md) | Validación del JWT en Identidad | `identidad` | 08 |
| [UT-CU02-BE-13](./UT-CU02-BE-13.md) | Validación del JWT en el gateway y 401/403 | `gateway` | 11 |
| [UT-CU02-BE-14](./UT-CU02-BE-14.md) | Validación del JWT en Escuela | `escuela` | 11 |
| [UT-CU02-BE-15](./UT-CU02-BE-15.md) | Validación del JWT en Pagos | `pagos` | 11 |
| [UT-CU02-BE-16](./UT-CU02-BE-16.md) | Validación del JWT en Académico | `academico` | 11 |
| [UT-CU02-BE-17](./UT-CU02-BE-17.md) | Prueba de integración del login y la renovación | `identidad` | 05, 06, 07, 08, 09, 10, 12 |

**Cadena:** `02 → 03 → 09`; `01 → 04 → 05, 06, 07, 08, 09, 10`; `08 → 11, 12`; `11 → 13, 14, 15, 16`; `05 + 06 + 07 + 08 + 09 + 10 + 12 → 17`. Las UT de Escuela (02, 03) son independientes de las de Identidad hasta la 09.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Login correcto: sesión, cookie web/cuerpo móvil, datos de la persona | 05, 08, 10, 17 |
| CA04, A2 | Credenciales incorrectas, mismo 401 | 05, 17 |
| CA06, CA07, A3 | Bloqueo a los 5 intentos y desbloqueo a los 15 minutos | 01, 05, 17 |
| CA08, A4 | Cuenta inactiva | 05, 17 |
| CA09, A5 | Escuela suspendida (Super admin exento) | 02, 03, 05, 09, 17 |
| CA10, A6 | Rol no permitido en la plataforma | 04, 05, 17 |
| CA11 | Una sesión activa por dispositivo | 05, 07, 17 |
| CA15, CA16, A8 | Renovación: token inválido/usado y rotación | 06, 07, 17 |
| CA17, A9 | Renovación rechazada por cuenta o escuela | 06, 17 |
| CA19 | Contraseña y tokens fuera de los logs | 05, 06, 17 |
| CA20 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| Sesión única entre servicios | Validar el token en gateway y servicios | 11 a 16 |

CA02, CA03, CA05, CA12 a CA14 y CA18 son de frontend y mobile.

## Follow-ups (no se implementan en estas UT)

- Cerrar sesión (CU03), recuperar contraseña (CU04) y el job que borra sesiones vencidas.
- Validación del JWT en Notificaciones (NestJS), Asistente IA (FastAPI) y Contenido (Laravel) con su primer endpoint.
- Filtro automático por `escuela_codigo` (diseño aparte antes de CU07 y CU08) y rotación de claves del JWT.
- Revocar todas las sesiones si se reutiliza un token ya rotado.

## Qué NO incluyen estas UT

- Menú por rol, permisos finos (`PERM_`) y gestión de roles (CU05).
- Cambios en el registro (CU01) más allá de reutilizar sus puertos.
- Autorización por rol en los endpoints de negocio (cada CU la agrega con `@PreAuthorize`).

## Commits sugeridos

Un commit `[BACK]` por servicio (`identidad`, `escuela`, `gateway`, `pagos`, `academico`); `docker-compose.yml` (`IDENTIDAD_URL` de UT 14 a 16) es `[INFRA]` y `docs/errors/` es `[DOCS]`.
