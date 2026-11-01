# UT-CU44-BE-14 — Académico: `ClienteDePagos`, una interfaz con dos credenciales

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-14` |
| **Nombre** | Académico: `ClienteDePagos`, una interfaz con dos credenciales |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-06` |

## Antes de implementar

Las decisiones 3, 14 y 16 del [README](./README.md#decisiones-de-diseño). Leer `PagosClient` (el de CU14, con Bearer) y `PagosServicioClient` (UT 06) completos: esta UT los pone detrás de una misma interfaz **sin cambiar el comportamiento de ninguno**. [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Deshacer una reserva y devolver una hora tiene que funcionar igual cuando el estudiante está presente (con su sesión) y cuando no (con la credencial del sistema), sin escribir dos caminos distintos.

## Objetivo

Existe `ClienteDePagos`, la interfaz que el orquestador usa para descontar y devolver horas, con una implementación para el Bearer del estudiante y otra para la credencial de servicio.

## Alcance

- `ClienteDePagos` (interfaz) en `features/reservas/client/`:
  - `void descontar(UUID escuelaCodigo, UUID estudianteCodigo, UUID instructorCodigo, UUID claseCodigo, int horas, String idempotencyKey)`: lanza `SinHorasException` (`409 ERR_PAG_09`) o `ServicioNoDisponibleException` (`ERR_ACA_17`), igual que los clientes actuales.
  - `void devolver(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey)`: un `404 ERR_PAG_16` **no** es error; cualquier otra falla lanza `ServicioNoDisponibleException`.
- `PagosServicioClient` (UT 06) pasa a **implementar** `ClienteDePagos` (solo se le agrega `implements` y los `@Override`). Es el cliente del job: usa `X-Service-Key`, no `Authorization`.
- `PagosConBearer` (nueva, en el mismo paquete, **no es un bean**): `new PagosConBearer(PagosClient pagosClient, String bearer)` implementa `ClienteDePagos` delegando en los métodos que `PagosClient` ya tiene, con el `bearer` recibido. **Ignora `escuelaCodigo`** (la sesión del estudiante ya lo determina) y `PagosClient` **no se modifica**.
- Una fábrica `ClienteDePagos conBearer(String bearer)` en un componente nuevo `ClientesDePagos` (`@Component`, junto a las anteriores), para que `ReservaService` y el orquestador obtengan el cliente del estudiante sin construir objetos a mano.
- `PagosServicioClient` queda inyectable como **el cliente de servicio por defecto** (`@Primary` no: se inyecta por su tipo concreto o con `@Qualifier("clienteDeServicio")`, para que nadie reciba el equivocado sin darse cuenta).
- Tests unitarios (servidor falso o `MockRestServiceServer`): cada implementación manda **su** credencial (`PagosConBearer` solo `Authorization`, `PagosServicioClient` solo `X-Service-Key`); ambas dan el mismo resultado ante `409 ERR_PAG_09`, `404 ERR_PAG_16` y `5xx`; `PagosConBearer` no cambia ninguna llamada de `PagosClient` (se compara con una llamada directa).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/ClienteDePagos.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/PagosConBearer.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/ClientesDePagos.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/PagosServicioClient.java` (solo `implements`)
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/client/` (tests)

## Fuera de alcance (no tocar)

- `PagosClient` y `ReservaService`: no cambian (la UT 16 hace que `ReservaService` use esto).
- El orquestador y los manejadores (UT 09).
- Consultar saldo.
- Cualquier cambio en Pagos.

## Resultado esperado

El orquestador puede pedir «descontar» y «devolver» sin saber con qué credencial habla, y CU15 podrá usar el mismo cliente tanto cuando cancela el estudiante como cuando cancela alguien más.

## Validación

1. `./mvnw test` en `academico` pasa, incluidos todos los tests de CU14 sin cambios.
2. `./mvnw checkstyle:check` sin errores.
3. `git diff` de `PagosClient.java` y `ReservaService.java` está vacío.
