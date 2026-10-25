# UT-CU26-BE-06 — Vencimiento de pagos pendientes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-06` |
| **Nombre** | Vencimiento de pagos pendientes |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU26-BE-05 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Si el estudiante abandona la página de pago, el pago no puede quedar pendiente para siempre: pasa a fallido y no acredita nada.

## Objetivo

Un proceso periódico marca como fallidos los pagos pendientes cuya página de pago ya venció.

## Alcance

- Un bean `java.time.Clock` en `config/` (si no existe) para poder fijar la hora en los tests, y `@EnableScheduling` en la aplicación.
- `PagoService.vencerPendientes(Instant ahora)`: busca por páginas de 100 los pagos `PENDIENTE` con `expira_en` anterior a `ahora - margenVencimientoMinutos` (por defecto 10, para dar tiempo a un aviso tardío de la pasarela) y los pasa a `FALLIDO`, cada uno en su transacción; devuelve cuántos cambió. Un pago que ya no está `PENDIENTE` al leerlo (llegó el aviso) se salta.
- `features/pagos/service/PagosVencidosJob` con `@Scheduled(fixedDelayString = "${app.pagos.vencimiento-cada-segundos:300}")` que llama al método; un error se registra `ERROR` y no detiene el proceso. Registra `INFO` solo cuando cambió algo (`module = pagos`, `cantidad`).
- `PagosProperties.Vencimiento` (prefijo `app.pagos.vencimiento`): `cadaSegundos` (por defecto 300, al menos 10) y `margenMinutos` (por defecto 10); variables `PAGOS_VENCIMIENTO_CADA_SEGUNDOS` y `PAGOS_VENCIMIENTO_MARGEN_MINUTOS`. El proceso arranca después de esa espera, no al levantar el servicio.
- Tests: con `Clock` fijo, un pago vencido pasa a `FALLIDO`, uno vigente no, uno ya `CONFIRMADO` no se toca, el margen se respeta y el proceso sigue si un pago falla al guardarse; `PagoRepositoryTest` de la consulta de vencidos (Testcontainers).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/PagoService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/PagosVencidosJob.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/RelojConfig.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/PagosApplication.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoVencimientoTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoRepositoryTest.java

## Fuera de alcance (no tocar)

- Avisar a la pasarela que expire la sesión: expira sola.
- Reintentar o revertir un cobro tardío (CU27 y CU31).
- Notificar al estudiante.

## Resultado esperado

Un pago pendiente que lleva más de la vigencia y el margen sin completarse queda fallido; los demás no cambian.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con `PAGOS_VENCIMIENTO_CADA_SEGUNDOS` bajo y `vigenciaSesionMinutos` mínimo, un pago creado y no pagado pasa a `FALLIDO` pasada la vigencia y el margen.
