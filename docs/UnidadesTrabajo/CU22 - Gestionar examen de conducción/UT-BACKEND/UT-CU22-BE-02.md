# UT-CU22-BE-02 — Repositorios y servicio de exámenes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-02` |
| **Nombre** | Repositorios y servicio de exámenes |
| **Caso de uso** | CU22-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU22-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Un examen mal programado (fecha pasada, estudiante ajeno, dos a la vez) no puede existir, y el resultado debe llegar al historial.

## Objetivo

La lógica de programar, listar, reprogramar, cancelar, marcar inasistencia y registrar el resultado de un examen.

## Alcance

- `ExamenConduccionRepository` (+ `Custom`/`Impl` con `MongoTemplate`, y `FiltroExamenes`) y `ResultadoRepository`.
- DTOs: `ProgramarExamenRequest`, `ReprogramarExamenRequest`, `RegistrarResultadoExamenRequest`, `ExamenResponse`.
- `ExamenConduccionService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`: reglas de intento, un programado por categoría, transiciones de estado, 70 puntos para aprobar y escritura de la nota.
- `ExamenConduccionServiceTest` (13 casos, repositorios y cliente mockeados, `Clock` fijo).

Archivos esperados (crear/tocar): los de `repository/`, `dto/` y `service/` del módulo `examenes`, `ResultadoRepository.java` y el test.

## Fuera de alcance (no tocar)

- El controller (UT-03).
- Validar evaluador y vehículo contra otros servicios.

## Resultado esperado

Ningún examen inválido se guarda y cada rechazo tiene su código.

## Validación

1. `./mvnw test -Dtest=ExamenConduccionServiceTest`.
2. `./mvnw checkstyle:check` sin errores.
