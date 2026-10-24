# Unidades de trabajo — CU07-BE

**Caso de uso:** Gestionar autos: registrar, listar, editar y cambiar el estado de los autos de la escuela, aislados por escuela.
**Microservicio(s) / micro-frontend(s):** `escuela` (Clean) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU07 - Gestionar autos.md`](../../../CasosDeUso/CU07%20-%20Gestionar%20autos.md)
**Recurso HTTP / contrato:** `/api/v1/vehiculos` (ya enrutado por el gateway a `escuela`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Filtro por tenant mínimo.** El diseño dejaba pendiente el filtro por escuela "antes de CU07". Se resuelve solo para `vehiculo`: columna `escuela_codigo` y filtro explícito en cada consulta del repositorio, con la escuela tomada del claim `escuela` del JWT en el controller. El mecanismo automático general queda para cuando haya más de una tabla que lo necesite.
- **Un auto de otra escuela es un 404** (`ERR_ESC_09`), no un 403: no se revela qué autos existen.
- **Un solo caso de uso para los tres cambios de estado** (`CambiarEstadoVehiculoUseCase`, destino como parámetro): son la misma regla (`EstadoVehiculo.permiteIrA`). Tres endpoints, un caso de uso.
- **Placa única en toda la plataforma** (el `UNIQUE` ya existía), no por escuela.
- **La foto se sube directo a Cloudinary** con una firma del backend (el API secret no sale de `escuela`); el auto guarda solo la URL. El seed usa la imagen pública `car` de la cuenta `demo`, así no hace falta cuenta propia para verlo.
- **Categoría de licencia e historial de mantenimiento fuera de alcance**: el catálogo de categorías no tiene caso de uso todavía.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/vehiculos` | 201 | `VehiculoResponse` |
| `GET` | `/api/v1/vehiculos?page&pageSize&search&filter[estado]` | 200 | `VehiculoResponse[]` + `meta` |
| `GET` | `/api/v1/vehiculos/{codigo}` | 200 | `VehiculoResponse` |
| `PATCH` | `/api/v1/vehiculos/{codigo}` | 200 | `VehiculoResponse` |
| `POST` | `/api/v1/vehiculos/{codigo}/enviar-a-mantenimiento` | 200 | `VehiculoResponse` |
| `POST` | `/api/v1/vehiculos/{codigo}/habilitar` | 200 | `VehiculoResponse` |
| `POST` | `/api/v1/vehiculos/{codigo}/dar-de-baja` | 200 | `VehiculoResponse` |
| `POST` | `/api/v1/vehiculos/firma-imagen` | 200 | `{ cloudName, apiKey, timestamp, folder, signature }` |

Códigos de error nuevos: `ERR_ESC_08` a `ERR_ESC_11` ([docs/errors](../../../errors/escuela/ERRORES_ESCUELA.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU07-BE-01](./UT-CU07-BE-01.md) | Migración y Model con `escuela_codigo` | — |
| [UT-CU07-BE-02](./UT-CU07-BE-02.md) | Dominio y repositorio de vehículos | 01 |
| [UT-CU07-BE-03](./UT-CU07-BE-03.md) | Casos de uso: registrar, listar y obtener | 02 |
| [UT-CU07-BE-04](./UT-CU07-BE-04.md) | Casos de uso: editar y cambiar estado | 02 |
| [UT-CU07-BE-05](./UT-CU07-BE-05.md) | Controller, errores y documentación | 03, 04 |
| [UT-CU07-BE-06](./UT-CU07-BE-06.md) | Foto del auto en Cloudinary y seed de ejemplo | 05 |

**Cadena:** `01 → 02 → 03, 04 → 05 → 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con búsqueda, filtro y paginación | 02, 03, 05 |
| F1 | Registrar un auto | 01, 02, 03, 05 |
| F2 | Editar | 04, 05 |
| F3, F4, F5 | Mantenimiento, habilitar y baja | 02, 04, 05 |
| A1 a A7 | Validación, placa en uso, km, 404, transición, rol, token sin escuela | 03, 04, 05 |

## Follow-ups (no se implementan en estas UT)

- Categoría de licencia del auto (cuando exista el CU del catálogo).
- Historial de mantenimiento (registrar y consultar `mantenimiento_vehiculo`).
- Filtro por tenant automático para el resto de las tablas de Escuela (CU08 en adelante).

## Qué NO incluyen estas UT

- Asignar autos a instructores (CU10) ni consultar disponibilidad (CU11).
- Cambios en el gateway: `vehiculos` ya está enrutado a `escuela`.
