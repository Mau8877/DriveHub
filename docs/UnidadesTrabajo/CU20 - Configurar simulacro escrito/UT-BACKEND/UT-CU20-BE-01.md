# UT-CU20-BE-01 — Modelo por escuela y códigos de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-01` |
| **Nombre** | Modelo por escuela y códigos de error |
| **Caso de uso** | CU20-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB, índices en código) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Cada escuela configura sus propios simulacros y ninguna ve los de otra.

## Objetivo

`SimulacroConfig` pertenece a una escuela y existen los códigos de error del módulo.

## Alcance

- `SimulacroConfig`: campos `escuelaCodigo` y `creadoPorCodigo`; índice compuesto `idx_simulacro_escuela_categoria_activo (escuelaCodigo, categoria, activo)`.
- `AcademicoErrorCodes`: `SIMULACRO_NO_ENCONTRADO` (`ERR_ACA_21`), `SIMULACRO_YA_EN_ESE_ESTADO` (`ERR_ACA_22`), `SIMULACRO_NOMBRE_DUPLICADO` (`ERR_ACA_23`).

Archivos esperados (crear/tocar): `SimulacroConfig.java`, `AcademicoErrorCodes.java`.

## Fuera de alcance (no tocar)

- `IntentoSimulacro` y su filtro por escuela (CU21).
- Repositorio y servicio (UT siguientes).

## Resultado esperado

El servicio compila y arranca con el índice nuevo.

## Validación

1. `./mvnw test` en `academico` (el contexto de Spring arranca).
2. La colección `simulacros_config` muestra el índice compuesto tras arrancar.
