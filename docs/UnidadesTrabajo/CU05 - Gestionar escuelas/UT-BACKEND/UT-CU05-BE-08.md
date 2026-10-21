# UT-CU05-BE-08 — `CrearEscuelaUseCase` (escuela y Administrador)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-08` |
| **Nombre** | `CrearEscuelaUseCase` (escuela y Administrador) |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-02, UT-CU05-BE-07 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y el CU: [CU05 - Gestionar escuelas](../../../CasosDeUso/CU05%20-%20Gestionar%20escuelas.md) (regla de la saga simplificada).

## Solicitud de negocio

Crear una escuela implica crear a su Administrador en otro servicio, sin una transacción que abarque las dos bases.

## Objetivo

El caso de uso crea la escuela y el Administrador juntos, y si algo falla no deja nada a medias.

## Alcance

- `application/usecase/CrearEscuelaUseCase.ejecutar(CrearEscuelaInput)` con `@PreAuthorize("hasRole('SUPER_ADMIN')")`; `application/dto/CrearEscuelaInput` (`nombre`, `slug`, datos del Administrador) y `EscuelaCreadaOutput` (la escuela y el Administrador creado). **No** lleva `@Transactional`: usa un `TransactionTemplate` para controlar el momento exacto de la confirmación.
- Pasos: 1) normaliza el slug (`trim` + minúsculas) y valida formato (`^[a-z0-9]+(-[a-z0-9]+)*$`, 3 a 60 caracteres) y nombre (3 a 120) → `ValidationException` `ERR_SYS_01`; 2) `existePorSlug` → `ConflictException` `ERR_ESC_03`; 3) genera el `codigo` de la escuela con `UuidV7`; 4) `transactionTemplate.execute`: `guardar` la escuela (con `saveAndFlush`) y, **dentro de la transacción aún abierta**, `gateway.crearAdministrador(codigo, datos)`; al salir se confirma. Una violación del `UNIQUE` del slug en ese `guardar` (dos peticiones a la vez) → `ConflictException` `ERR_ESC_03`.
- **Saga simplificada, sin estado:** el orden hace que casi todo fallo sea inofensivo. Si Identidad rechaza (una excepción de negocio: correo en uso, menor de edad...), la transacción se deshace y no hay nada que compensar; el error de Identidad llega tal cual. Si Identidad no responde, vence o contesta 5xx (`IdentidadNoDisponibleException`), o falla la confirmación, el resultado en Identidad es incierto: se llama a `gateway.revertirAdministrador(codigo)` (idempotente) y se relanza el error original. Si la compensación también falla se registra `ERROR` ("Compensación fallida", con `escuelaCodigo`) y se relanza igual el error original: la cuenta sobrante no puede iniciar sesión porque su escuela no existe, y queda registrada para limpiarla.
- `EscuelaErrorCodes.SLUG_EN_USO = "ERR_ESC_03"`, documentado en `docs/errors/escuela/ERRORES_ESCUELA.md` y `docs/errors/README.md` (409, `ConflictException`, "Ya existe una escuela, activa o inactiva, con ese slug", `CrearEscuelaUseCase.ejecutar()`).
- Logs: `INFO` "Escuela creada" (`escuelaCodigo`, `slug`); `WARN` con `errorCode` en los rechazos; nunca la contraseña.
- Tests unitarios (gateway y repositorio mockeados; `TransactionTemplate` sobre un `PlatformTransactionManager` simulado, para verificar commit y rollback): caso feliz (orden: guardar → crear admin → commit; el `codigo` pasado al gateway es el de la escuela); slug en uso (no llama al gateway); rechazo de negocio de Identidad (rollback, sin compensación); `IdentidadNoDisponibleException` (rollback + `revertirAdministrador`); fallo del commit (compensa); compensación que falla (se propaga el error original); la contraseña no aparece en los logs.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/CrearEscuelaUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/CrearEscuelaInput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/EscuelaCreadaOutput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/CrearEscuelaUseCaseTest.java`
- `docs/errors/escuela/ERRORES_ESCUELA.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El controller (UT 12) y los demás casos de uso de gestión.
- Editar al Administrador o enviarle correo.
- Persistir el estado de la saga ni usar outbox o RabbitMQ.

## Resultado esperado

Crear una escuela con su Administrador es todo o nada, o falla sin dejar nada que pueda usarse.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
