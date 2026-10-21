# UT-CU02-BE-05 — `IniciarSesionService` y errores `ERR_IDE_04` a `ERR_IDE_08`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-05` |
| **Nombre** | `IniciarSesionService` y errores `ERR_IDE_04` a `ERR_IDE_08` |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-04 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Es la lógica del inicio de sesión: credenciales, bloqueo por intentos, cuenta y escuela activas, plataforma permitida y sesión única por dispositivo.

## Objetivo

`IniciarSesionService` implementa `IniciarSesionUseCase` con todas las reglas del CU, probado con los puertos mockeados.

## Alcance

- `IdentidadErrorCodes`: `CREDENCIALES_INCORRECTAS = ERR_IDE_04` (401 `UnauthorizedException`), `CUENTA_BLOQUEADA = ERR_IDE_05` (423 `LockedException`), `CUENTA_INACTIVA = ERR_IDE_06` (403 `ForbiddenException`), `ESCUELA_SUSPENDIDA = ERR_IDE_07` (403), `PLATAFORMA_NO_PERMITIDA = ERR_IDE_08` (403).
- `application/service/IniciarSesionService` con `@Transactional(noRollbackFor = BusinessException.class)` (si no, el contador de intentos fallidos se desharía junto con la excepción). Orden: 1) normaliza el correo (`trim` + minúsculas); si no existe, `ERR_IDE_04` sin tocar contadores. 2) Si la cuenta está `BLOQUEADO` y `bloqueadoHasta` es futuro, `ERR_IDE_05` con el mensaje de cuántos minutos faltan; si ya pasó, la desbloquea (`ACTIVO`, intentos 0, `bloqueadoHasta` nulo). 3) Si la contraseña no coincide, suma un intento; al llegar al 5.º fallido consecutivo pasa a `BLOQUEADO`, `bloqueadoHasta = ahora + 15 min` y responde `ERR_IDE_05`; si no, `ERR_IDE_04`. 4) Con la contraseña correcta: `INACTIVO` → `ERR_IDE_06`; si tiene escuela, consulta `EscuelaConsultaPort.buscarPorCodigo` y, si no existe o `activa=false`, `ERR_IDE_07` (el `SUPER_ADMIN` no tiene escuela y se salta este paso; un rol distinto sin escuela también es `ERR_IDE_07`); `AccesoPorPlataforma` no permite → `ERR_IDE_08` con el mensaje de la plataforma correcta. 5) Reinicia intentos, `ultimoLogin = ahora`, `revocarActivos(usuario, dispositivo)`, genera el token opaco, guarda su hash con vigencia de 7 días, emite el JWT de 15 minutos y devuelve `SesionEmitida`.
- Propiedades: `app.auth.max-intentos=5`, `app.auth.bloqueo-minutos=15`, `app.auth.access-ttl-minutos=15`, `app.auth.refresh-ttl-dias=7` (inyectadas con `@Value`).
- Logs solo en el servicio: `INFO` al iniciar sesión (`module="auth"`, `usuarioCodigo`, `dispositivo`), `WARN` con `errorCode` en cada rechazo. Nunca la contraseña, el hash ni los tokens.
- Documentar `ERR_IDE_04` a `ERR_IDE_08` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice.
- Tests unitarios (puertos mockeados, `Clock` fijo): éxito web (admin) y móvil (estudiante) con los datos que se guardan; correo inexistente no suma nada; 4 fallos suman y el 5.º bloquea con 423; bloqueada con contraseña correcta sigue en 423; bloqueo vencido se levanta y entra; inactiva, escuela suspendida, escuela inexistente, Super admin sin escuela, estudiante en web y admin en móvil; el login revoca solo la sesión previa del mismo dispositivo; los logs no contienen contraseña.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `.../application/service/IniciarSesionService.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `.../application/service/IniciarSesionServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Renovación de sesión (UT 06) y cierre de sesión (CU03).
- Los adaptadores de persistencia, JWT y Escuela (UT 07 a 09) y el controller (UT 10).
- Recuperación de contraseña (CU04).
- Limitar por IP el login (lo cubre el rate limit del gateway).

## Resultado esperado

Con puertos de prueba, el servicio abre sesión solo cuando corresponde y rechaza con el código correcto cada caso inválido, contando y bloqueando intentos.

## Validación

1. `./mvnw test` en `identidad` pasa con `IniciarSesionServiceTest` en verde.
2. `ERRORES_IDENTIDAD.md` tiene las cinco secciones nuevas con HTTP, excepción, significado y dónde se lanza (`IniciarSesionService.iniciar()`).
3. `./mvnw checkstyle:check` sin errores.
