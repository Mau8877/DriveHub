# UT-CU07-BE-04 — Casos de uso: editar y cambiar estado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-04` |
| **Nombre** | Casos de uso: editar y cambiar estado |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU07-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

La flota cambia: se corrigen datos, los autos entran y salen de mantenimiento y algunos se dan de baja.

## Objetivo

Editar los campos permitidos y mover el auto entre estados respetando las transiciones.

## Alcance

- `EditarVehiculoInput` y `EditarVehiculoUseCase`: un campo nulo no se toca; no hay parámetro para placa, transmisión ni estado; rechaza el auto de baja (`ERR_ESC_09`) y un kilometraje menor al actual (`ERR_SYS_01`).
- `CambiarEstadoVehiculoUseCase.ejecutar(escuela, codigo, destino)`: 404 `ERR_ESC_08`; transición no permitida `ERR_ESC_09`; `INFO` con estado de origen y destino.

Archivos esperados (crear/tocar): los dos casos de uso, `EditarVehiculoInput` y sus tests (`EditarVehiculoUseCaseTest`, `CambiarEstadoVehiculoUseCaseTest`).

## Fuera de alcance (no tocar)

- Registrar y listar (UT-03).
- Registrar el mantenimiento realizado (historial): caso de uso aparte.

## Resultado esperado

Ninguna edición ni cambio de estado pasa sobre un auto de baja ni sobre uno de otra escuela.

## Validación

1. `./mvnw test -Dtest='EditarVehiculoUseCaseTest,CambiarEstadoVehiculoUseCaseTest'` en verde.
2. Editar solo `color` deja marca, modelo, km y vencimientos como estaban.
