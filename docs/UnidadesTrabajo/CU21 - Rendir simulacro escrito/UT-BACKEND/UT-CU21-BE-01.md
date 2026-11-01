# UT-CU21-BE-01 — Modelos, repositorios y códigos de error del intento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-01` |
| **Nombre** | Modelos, repositorios y códigos de error del intento |
| **Caso de uso** | CU21-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Un intento guarda lo que se le mostró al estudiante, y se arma con las preguntas del banco de su escuela.

## Objetivo

Existen los modelos con copia, las consultas del intento y de las preguntas por tema, y los códigos de error.

## Alcance

- `IntentoSimulacro` (escuela, simulacro, copias, setters) y `PreguntaIntento` (código, tipo, explicación).
- `IntentoSimulacroRepository` (intento por estudiante, en curso por simulacro, historial paginado sin los en curso).
- `PreguntaRepositoryCustom.activasDelTema` y `SimulacroConfigRepository...ActivoTrue...` (los activos de la escuela).
- DTOs: `SimulacroDisponibleResponse`, `IniciarIntentoRequest`, `FinalizarIntentoRequest`, `IntentoIniciadoResponse`, `IntentoResultadoResponse`, `IntentoResumenResponse`.
- `AcademicoErrorCodes`: `ERR_ACA_28` a `ERR_ACA_31`.

Archivos esperados (crear/tocar): los de `model/`, `repository/` y `dto/` del módulo `simulacros`, `PreguntaRepositoryCustom(Impl)` y `AcademicoErrorCodes.java`.

## Fuera de alcance (no tocar)

- La lógica de calificación y los endpoints (UT siguientes).

## Resultado esperado

El servicio compila y arranca con los modelos nuevos.

## Validación

1. `./mvnw test` en `academico` y `./mvnw checkstyle:check`.
