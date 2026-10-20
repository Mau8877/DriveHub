# UT-CU04-BE-03 — `SolicitarRecuperacionService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-03` |
| **Nombre** | `SolicitarRecuperacionService` |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04-BE-01 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Este CU se apoya en lo que construye [CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md) (`EnviadorCorreoPort`, tokens con `tipo`, `correo_verificado`, `ERR_IDE_11`, rutas `/cuenta`): debe estar implementado antes de empezar.

## Solicitud de negocio

La persona pide el enlace de recuperación sin que el sistema revele qué correos existen ni cuáles están verificados.

## Objetivo

`SolicitarRecuperacionService` envía el enlace solo a cuentas válidas y no delata a las demás.

## Alcance

- `domain/port/in/SolicitarRecuperacionUseCase` con `void solicitar(String email)` (el correo se normaliza: sin espacios y en minúsculas).
- `application/service/SolicitarRecuperacionService`, `@Transactional`: busca la cuenta; si no existe, no está `ACTIVO` o `correoVerificado` es `false`, termina sin hacer nada. Si sirve: `invalidarPendientes(usuario, RECUPERACION)`, genera el token, guarda el hash (`tipo = RECUPERACION`, vigencia `app.auth.recuperacion-ttl-minutos` = 30) y envía el enlace `${app.web.url}/cuenta/restablecer?token=<token en claro>`.
- Nunca lanza una excepción por el estado de la cuenta ni por un fallo del envío: ante `EnviadorCorreoPort` fallido registra `ERROR` (`module="auth"`, `usuarioCodigo`) y termina normalmente.
- Logs: `INFO` al enviar (`usuarioCodigo`); los rechazos silenciosos no se registran con el correo recibido. Nunca el token ni el enlace.
- Tests unitarios: cuenta válida → guarda hash, invalida pendientes y envía el enlace correcto; correo inexistente, cuenta inactiva y cuenta sin verificar → no guardan ni envían y no lanzan; fallo del envío no lanza; el correo se normaliza.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/SolicitarRecuperacionUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/SolicitarRecuperacionService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/SolicitarRecuperacionServiceTest.java`
- `projects/backend/identidad/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Restablecer la contraseña (UT 04) y los endpoints (UT 05).
- Igualar el tiempo de respuesta entre cuentas existentes e inexistentes.

## Resultado esperado

Pedir la recuperación envía el enlace solo a cuentas activas con el correo verificado y no filtra información a quien lo pide.

## Validación

1. `./mvnw test` en `identidad` pasa con `SolicitarRecuperacionServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
