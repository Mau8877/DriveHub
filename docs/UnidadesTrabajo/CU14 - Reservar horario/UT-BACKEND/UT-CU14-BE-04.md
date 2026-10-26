# UT-CU14-BE-04 — Clientes de Pagos e Identidad en Académico

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-04` |
| **Nombre** | Clientes de Pagos e Identidad en Académico |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (entre servicios no hay dependencia de código: se llama por REST), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (`ServicioNoDisponibleException` y documentar el código), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca el token). Tomar como modelo `pagos/.../client/IdentidadClient.java` y `ServicioNoDisponibleException` de Pagos. Contrato de Pagos: [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md).

## Solicitud de negocio

Reservar una clase gasta una hora de la billetera (que es de Pagos), y mostrar al instructor por su nombre requiere preguntarle a Identidad. Académico no puede leer sus bases.

## Objetivo

Existen en Académico un cliente de la billetera de Pagos y un cliente de nombres de Identidad, con la falla de red o de servicio traducida a un error propio.

## Alcance

- `common/exception/ServicioNoDisponibleException` (503), copia de la de Pagos, y la constante `SERVICIO_NO_DISPONIBLE = "ERR_ACA_17"` en `AcademicoErrorCodes`. Manejarla en el `GlobalExceptionHandler` como el resto de las `BusinessException`. Documentar `ERR_ACA_17` en `docs/errors/academico/ERRORES_ACADEMICO.md` y en `docs/errors/README.md` (mensaje al cliente: «No pudimos completar la operación. Inténtalo de nuevo más tarde.»).
- `features/reservas/client/PagosClient` (`@Component`, `RestClient` con timeout; URL `app.pagos.url` ← `PAGOS_URL`, timeout `app.pagos.timeout-segundos`, 5 por defecto), siempre reenviando el `Bearer` del estudiante recibido como parámetro:
  - `int saldo(String bearer)`: `GET /api/v1/billeteras-horas/mia`.
  - `ResultadoConsumo descontar(String bearer, UUID estudianteCodigo, UUID instructorCodigo, UUID claseCodigo, int horas, String idempotencyKey)`: `POST /api/v1/billeteras-horas/consumos`. Devuelve `horasDescontadas` y `horasDisponibles`. Un `409` con `ERR_PAG_09` se traduce a una excepción propia del paquete `SinHorasException`; cualquier otro fallo (5xx, 4xx distinto, timeout, corte de red, respuesta incompleta) es `ServicioNoDisponibleException`.
  - `void devolver(String bearer, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey)`: `POST /api/v1/billeteras-horas/devoluciones`. Un `404` con `ERR_PAG_16` (no había nada que devolver) **no es error**; el resto de fallos es `ServicioNoDisponibleException`.
- `features/reservas/client/IdentidadClient`: `Map<UUID, String> nombresDeInstructores(List<UUID> codigos, String bearer)`: `POST /api/v1/usuarios/instructores/nombres` (UT 01) en lotes de 100, sin repetidos; los que Identidad no devuelve no están en el mapa. Falla → `ServicioNoDisponibleException`. URL `app.identidad.url` ← `IDENTIDAD_URL` (la variable ya existe en el servicio).
- `application.properties`: `app.pagos.url=${PAGOS_URL:http://localhost:8083}`, `app.pagos.timeout-segundos`, `app.identidad.url` y su timeout.
- `docker-compose.yml`: agregar `PAGOS_URL=http://drivehub-pagos:8083` al servicio `academico` (esto es un cambio de `INFRA`: va en su propio commit).
- El `Bearer` se lee con un helper `BearerActual` (copia del de Pagos, `config/`) a partir del request actual; **nunca** se loguea ni el token ni el cuerpo de la respuesta, solo códigos.
- Tests con un servidor falso (`HttpServer` del JDK o `MockRestServiceServer`): saldo y descuento felices; `409 ERR_PAG_09` → `SinHorasException`; `5xx`, timeout y respuesta incompleta → `ServicioNoDisponibleException`; `devolver` con `404 ERR_PAG_16` no lanza; nombres en más de un lote; el `Authorization` y el `Idempotency-Key` viajan en la petición.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/ServicioNoDisponibleException.java`, `AcademicoErrorCodes.java`, `GlobalExceptionHandler.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/PagosClient.java`, `IdentidadClient.java`, `SinHorasException.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/BearerActual.java`
- `projects/backend/academico/src/main/resources/application.properties`
- `docker-compose.yml`
- `docs/errors/academico/ERRORES_ACADEMICO.md`, `docs/errors/README.md`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/client/` (tests de ambos clientes)

## Fuera de alcance (no tocar)

- Cualquier cambio en Pagos o en Identidad (la UT 01 agrega el endpoint de nombres; Pagos ya tiene todo lo que se usa).
- La lógica de reservar y los demás códigos de error (`ERR_ACA_18` a `ERR_ACA_20`: UT 06).
- Reintentos automáticos: un fallo se informa, no se reintenta solo.

## Resultado esperado

Académico puede consultar el saldo, descontar y devolver horas de un estudiante, y obtener nombres de instructores, y distingue «no hay horas» de «el servicio no responde».

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. `docker compose config` muestra `PAGOS_URL` en `academico`.
