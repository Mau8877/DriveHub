# UT-CU12-MOB-09 — `HorariosService`: estados, pausar, reanudar y día completo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-09` |
| **Nombre** | `HorariosService`: estados, pausar, reanudar y día completo |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | `UT-CU12-MOB-08` |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md). Contrato en el [README de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http).

## Solicitud de negocio

La app tiene que poder pausar y reanudar horarios, consultar la semana y bloquear un día.

## Objetivo

`HorariosService` llama a los endpoints nuevos del backend.

## Alcance

- `listarPlantillas({Set<EstadoPlantilla>? estados, page, pageSize})`: manda `filter[estado]` repetido por cada estado (sin estados, no manda nada y el backend devuelve activos y pausados). Reemplaza al parámetro `activa`.
- `pausarPlantilla(codigo)` y `reanudarPlantilla(codigo)`: `POST /plantillas-horario/{codigo}/pausar` y `/reanudar`.
- `listarFranjas({DateTime? desde, DateTime? hasta, estado, page, pageSize})`: manda `filter[fechaDesde]` y `filter[fechaHasta]`; el parámetro `fecha` pasa a ser `desde = hasta`. Con una semana se pide `pageSize: 100`.
- `bloquearDia(DateTime fecha)` y `desbloquearDia(DateTime fecha)`: `POST /franjas/dias/{yyyy-MM-dd}/bloquear` y `/desbloquear`; devuelven un `DiaBloqueado`.
- Mismo manejo de errores que el resto (`ApiException` con su código).
- Tests con un adaptador falso de Dio (como el servicio actual): URL, método y parámetros de cada llamada, con varios estados repetidos; un `ERR_ACA_11` o `ERR_ACA_13` llega como `ApiException`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/services/horarios_service.dart`
- `projects/mobile/test/features/horarios/horarios_service_test.dart`
- `projects/mobile/test/helpers/horarios_falsos.dart`

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 10 a 12).

## Resultado esperado

El servicio cubre todo el contrato nuevo del backend con sus errores.

## Validación

1. `flutter test` pasa con los casos nuevos en verde.
2. `flutter analyze` sin avisos.
