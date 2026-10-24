# UT-CU07-BE-01 — Migración y Model con `escuela_codigo`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-01` |
| **Nombre** | Migración y Model con `escuela_codigo` |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway con timestamp, `ddl-auto=validate`).

## Solicitud de negocio

Cada escuela solo debe ver sus autos: el auto tiene que saber a qué escuela pertenece.

## Objetivo

La tabla `vehiculo` y `VehiculoModel` guardan la escuela dueña de cada auto.

## Alcance

- Migración `V20261024100000__vehiculo_escuela_codigo.sql`: `ALTER TABLE vehiculo ADD COLUMN escuela_codigo UUID NOT NULL` + índice `idx_vehiculo_escuela_codigo`. Es `NOT NULL` porque la tabla estaba vacía (CU07 es el primer caso de uso que la usa).
- `VehiculoModel`: campo `escuelaCodigo` (`updatable = false`) y constructor `(codigo, escuelaCodigo, placa, marca, modelo, transmision)` que deja el estado en `DISPONIBLE`.
- Actualizar `docs/DriveHub/DISENO_BASE_DE_DATOS.md` (columna, migración y pendiente de tenant).

Archivos esperados (crear/tocar): la migración, `VehiculoModel.java`, `DISENO_BASE_DE_DATOS.md`.

## Fuera de alcance (no tocar)

- `escuela_codigo` en `estudiante`, `instructor` y las demás tablas: lo suman CU08 y siguientes.
- Una FK hacia `escuela`: las referencias al tenant son por código, sin FK.

## Resultado esperado

El servicio arranca con `ddl-auto=validate` y la tabla `vehiculo` tiene la columna indexada.

## Validación

1. `./mvnw test` en `escuela` con Docker activo: `GestionVehiculosIntegrationTest` levanta Postgres, aplica Flyway y valida el esquema contra los Models.
2. Con el stack levantado, el log de `escuela` muestra la versión de esquema `20261024100000`.
