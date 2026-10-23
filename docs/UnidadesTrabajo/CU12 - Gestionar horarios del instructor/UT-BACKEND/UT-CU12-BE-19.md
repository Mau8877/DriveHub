# UT-CU12-BE-19 — FranjaService: consulta por rango y bloqueo de un día

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-19` |
| **Nombre** | FranjaService: consulta por rango y bloqueo de un día |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-14`, `UT-CU12-BE-16` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Leer la [decisión de diseño 14](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor ve la semana de un vistazo y, si un día no puede atender, lo bloquea entero de una vez.

## Objetivo

`FranjaService` lista franjas por rango de fechas y bloquea o desbloquea todas las de un día.

## Alcance

- `listar(UUID instructorCodigo, LocalDate fechaDesde, LocalDate fechaHasta, EstadoFranja estado, int page, int pageSize)`: sin fechas, desde hoy en adelante (como antes); con las dos, el rango inclusivo; con una sola, o con la inicial posterior a la final, o de más de 7 días (contando ambos extremos) → `ValidationException` `ERR_ACA_13`.
- `bloquearDia(UUID instructorCodigo, LocalDate fecha)` y `desbloquearDia(...)`: con `@PreAuthorize`; si la fecha es anterior a hoy → `ConflictException` `ERR_ACA_10`; llaman a `cambiarEstadoDelDia` (`LIBRE`→`BLOQUEADA` y `BLOQUEADA`→`LIBRE`), pasando la hora actual como `horaMinima` solo cuando la fecha es hoy. Devuelven un `DiaBloqueadoResponse(LocalDate fecha, long franjasAfectadas)` (DTO nuevo). Cero franjas no es un error. Log `INFO` con la fecha y la cantidad.
- Tests: rango válido, extremo inclusivo, 8 días → `ERR_ACA_13`, una sola fecha → `ERR_ACA_13`; bloquear día solo toca libres; hoy usa la hora actual como mínimo; fecha pasada → `ERR_ACA_10`; repetir no cambia nada.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/DiaBloqueadoResponse.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/FranjaServiceTest.java`

## Fuera de alcance (no tocar)

- Bloquear una franja suelta: no cambia.
- El controller y la ruta (UT 20).

## Resultado esperado

La consulta acepta un rango de hasta 7 días y se puede bloquear o desbloquear un día completo.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
