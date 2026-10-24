# UT-CU07-BE-02 — Dominio y repositorio de vehículos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-02` |
| **Nombre** | Dominio y repositorio de vehículos |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU07-BE-01 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (el dominio no importa Spring ni JPA; dos modelos por concepto).

## Solicitud de negocio

Las reglas del auto (estados permitidos, aislamiento por escuela) tienen que vivir en el dominio, no en la base ni en la pantalla.

## Objetivo

La entidad `Vehiculo`, la regla de transiciones de estado y el repositorio filtrado por escuela, con su implementación JPA.

## Alcance

- `EstadoVehiculo.permiteIrA(destino)`: `DISPONIBLE ⇄ MANTENIMIENTO`, ambos `→ BAJA`, `BAJA` final.
- `Vehiculo`: sumar `escuelaCodigo` y `creadoEn`, y `toBuilder = true`.
- `domain/repository/VehiculoRepository` (`existePorPlaca`, `guardar`, `buscarPorCodigo(escuela, codigo)`, `listar(escuela, filtro, pagina, tamano)`, `actualizar`) y `FiltroVehiculos(busqueda, estado)`.
- `VehiculoJpaRepository` (listado en SQL nativo con `CAST(:x AS text)` para los nulos) y `VehiculoRepositoryImpl`: la placa repetida en una carrera se traduce a `ConflictException(ERR_ESC_07)`; `actualizar` solo copia los campos editables y el estado, nunca placa, transmisión ni escuela; la búsqueda escapa `%`, `_` y `\`.
- Constante `PLACA_EN_USO` y las otras tres en `EscuelaErrorCodes`.

Archivos esperados (crear/tocar): `EstadoVehiculo`, `Vehiculo`, `VehiculoRepository`, `FiltroVehiculos`, `VehiculoJpaRepository`, `VehiculoRepositoryImpl`, `EscuelaErrorCodes`, `EstadoVehiculoTest`.

## Fuera de alcance (no tocar)

- La categoría de licencia: el mapeo al dominio la deja en `null`.
- Los casos de uso y el controller (UT-03 a UT-05).

## Resultado esperado

Un repositorio que nunca devuelve ni modifica autos de otra escuela.

## Validación

1. `EstadoVehiculoTest` cubre las cuatro transiciones permitidas y el resto prohibido.
2. `GestionVehiculosIntegrationTest.ca01_ca09_...` y `...elListadoFiltraPorEstadoYBusca...` pasan contra Postgres real.
