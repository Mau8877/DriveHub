# UT-CU44-BE-06 — Académico: cliente de Pagos con credencial de servicio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-06` |
| **Nombre** | Académico: cliente de Pagos con credencial de servicio |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-03` |

## Antes de implementar

Las decisiones 3, 5 y 7 del [README](./README.md#decisiones-de-diseño) y su tabla de contrato. Leer `PagosClient` completo: este cliente sigue su mismo estilo (`RestClient`, timeouts, manejo de errores, logging sin datos sensibles). [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md).

## Solicitud de negocio

El job de Académico tiene que hablar con Pagos sin la sesión de ningún estudiante, identificándose como Académico.

## Objetivo

Existe `PagosServicioClient`, que descuenta y devuelve horas por las rutas internas de Pagos con el header `X-Service-Key`, con el mismo manejo de errores que `PagosClient`.

## Alcance

- Propiedad `app.pagos.credencial-servicio` en `application.properties` con valor `${PAGOS_CREDENCIAL_ACADEMICO:}`.
- `PagosServicioClient` (`@Component`) en `features/reservas/client/`, junto a `PagosClient`, con la misma `app.pagos.url` y el mismo timeout (`app.pagos.timeout-segundos`):
  - `ResultadoConsumo descontar(UUID escuelaCodigo, UUID estudianteCodigo, UUID instructorCodigo, UUID claseCodigo, int horas, String idempotencyKey)`: `POST /internal/v1/billeteras-horas/consumos` con `X-Service-Key` y `Idempotency-Key`. Reutiliza el `record ResultadoConsumo` de `PagosClient`.
  - `void devolver(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey)`: `POST /internal/v1/billeteras-horas/devoluciones`.
  - `409 ERR_PAG_09` → `SinHorasException` (ya existe). `404 ERR_PAG_16` al devolver **no es error** (no había nada que devolver). Cualquier otra falla (5xx, 4xx incluido `401 ERR_PAG_19`, timeout, corte, respuesta incompleta) → `ServicioNoDisponibleException` con `ERR_ACA_17`. Un `401` o `403` de Pagos (credencial rechazada) además registra un `ERROR` (no se arregla sola); el resto, un `WARN`. El orquestador trata todos estos casos igual: «Pagos no responde», que **no** cuenta como intento fallido (UT 09 y 10).
  - Si la credencial configurada está vacía, **no sale ninguna petición**: lanza `ServicioNoDisponibleException` y registra un `ERROR` (no se arregla sola). Nunca se manda un header vacío.
  - El log registra solo operación y tipo de error, **nunca** la credencial ni el cuerpo de la respuesta.
- Tests unitarios con un servidor falso (`HttpServer` del JDK, como el test del gateway) o `MockRestServiceServer`: el header `X-Service-Key` y `Idempotency-Key` salen en cada llamada y **no** sale `Authorization`; descuento correcto; `409 ERR_PAG_09` → `SinHorasException`; devolver con `404 ERR_PAG_16` → sin error; `401`, `5xx` y timeout → `ServicioNoDisponibleException`; credencial vacía → ninguna petición y `ServicioNoDisponibleException`; la credencial no aparece en los logs.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/client/PagosServicioClient.java`
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/client/PagosServicioClientTest.java`

## Fuera de alcance (no tocar)

- `PagosClient` (el de la saga en vivo, con Bearer) y `ReservaService`: no cambian.
- Consultar saldo: el job no lo necesita.
- Quién llama a este cliente (UT 09).
- La variable de entorno del compose (UT 05).
- Cualquier cambio en Pagos.

## Resultado esperado

Académico puede pedir a Pagos descontar y devolver horas sin sesión, y si Pagos o la credencial fallan lo trata como "Pagos no disponible".

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Revisar los logs de los tests: ninguna línea contiene el valor de la credencial.
