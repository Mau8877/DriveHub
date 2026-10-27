# UT-CU44-BE-11 — Académico: job programado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-11` |
| **Nombre** | Académico: job programado |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-10` |

## Antes de implementar

Las decisiones 2, 7 y 8 del [README](./README.md#decisiones-de-diseño). Leer `ExtensionFranjasJob` (patrón de `@Scheduled` con cron y zona configurables) y `SchedulingConfig`. [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Las reservas a medias deben resolverse solas, aunque ningún estudiante abra la app.

## Objetivo

Un job de Académico revisa periódicamente las reservas `PENDIENTE_PAGO` antiguas de todos los estudiantes y las procesa con el orquestador.

## Alcance

- `OrquestadorProperties` (UT 09 y 10) suma: `habilitado` (por defecto `true`), `cron` (por defecto `0 * * * * *`, cada minuto) y `lote` (por defecto `50`).
- `OrquestadorClasesJob` (`@Component`) en `features/reservas/service/`, junto a `OrquestadorClases`:
  - `@Scheduled(cron = "${app.orquestador.cron}", zone = "${app.horarios.zona-horaria}")` y `@ConditionalOnProperty(prefix = "app.orquestador", name = "habilitado", matchIfMissing = true)`.
  - En cada pasada: `pendientesAntiguas(ahora − minutosParaRetomar, lote)` con `app.reservas.minutos-para-retomar` y el `Clock` inyectado, y `OrquestadorClases.procesar(...)` por cada una.
  - Un fallo en una reserva **no** frena a las demás ni a la próxima pasada: se captura por reserva.
  - Al terminar, **un** `INFO` con el resumen (`module: reservas`, `revisadas`, `confirmadas`, `canceladas`, `pendientes`, `sinRespuestaDePagos`, `enRevision`); si no había ninguna, no loguea nada (no llena el log cada minuto).
  - Sin sesión: no usa `SecurityContext`, no lleva `@PreAuthorize`.
- Una pasada que sigue en curso cuando llega la siguiente no se superpone: dejar un `AtomicBoolean`/guarda para que la nueva se omita (un solo proceso, decisión 8).
- Tests unitarios (orquestador simulado): procesa todas las pendientes que devuelve el repositorio; usa el umbral y el lote configurados; una excepción en una no impide procesar la siguiente; el resumen cuenta bien (incluidas las que esperan a Pagos); sin pendientes, no loguea; una pasada no se superpone con otra.
- Test `@SpringBootTest` con `habilitado=false`: el job no existe como bean.
- Test con `SecurityContext` vacío: el job corre sin error (CA09).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClasesJob.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/OrquestadorProperties.java`
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (tests)

## Fuera de alcance (no tocar)

- La lógica de retomar y la de intentos (UT 09 y 10): el job solo las llama.
- `ExtensionFranjasJob` y `SchedulingConfig`.
- Bloqueo distribuido entre réplicas (follow-up).
- Cancelaciones y reprogramaciones (CU15).
- Un endpoint para disparar el job a mano.

## Resultado esperado

Cada minuto, sin que nadie esté conectado, Académico resuelve las reservas que quedaron a medias.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con `app.orquestador.habilitado=false` el job no corre; con `true`, en el log aparece el `INFO` de resumen solo cuando había reservas por revisar.
