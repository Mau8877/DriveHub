# UT-CU21-BE-02 — Servicio de intentos: iniciar, finalizar, historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-02` |
| **Nombre** | Servicio de intentos: iniciar, finalizar, historial |
| **Caso de uso** | CU21-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU21-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

El estudiante rinde su simulacro con tiempo límite y recibe una calificación justa.

## Objetivo

La lógica de armar el intento, calificarlo, expirarlo y consultarlo.

## Alcance

- `IntentoSimulacroService` con `@PreAuthorize("hasRole('ESTUDIANTE')")`: `disponibles`, `iniciar` (preguntas al azar por regla, un intento en curso, expira los vencidos), `finalizar` (gracia de 30 s, calificación exacta, copia de respuestas y nota consolidada), `listarMios` y `obtener`.
- `IntentoSimulacroServiceTest` (13 casos, repositorios mockeados y `Clock` fijo).

Archivos esperados (crear/tocar): `IntentoSimulacroService.java` y su test.

## Fuera de alcance (no tocar)

- El controller (UT-03) y la pantalla móvil.

## Resultado esperado

Ningún intento con tiempo vencido cuenta y las respuestas correctas solo salen al terminar.

## Validación

1. `./mvnw test -Dtest=IntentoSimulacroServiceTest`.
2. `./mvnw checkstyle:check` sin errores.
