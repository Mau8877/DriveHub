# UT-CU44-BE-12 — Prueba de punta a punta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-12` |
| **Nombre** | Prueba de punta a punta |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` + `pagos` + `gateway` (solo se prueba, no se cambia código) |
| **Depende de** | `UT-CU44-BE-03`, `UT-CU44-BE-04`, `UT-CU44-BE-05`, `UT-CU44-BE-11` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Seguir el estilo de la prueba de punta a punta de CU14 (`UT-CU14-BE-10`): stack real con Docker Compose, datos temporales, y limpiar al terminar.

## Solicitud de negocio

Confirmar que las piezas funcionan juntas contra los servicios reales, no solo cada una con simulados.

## Objetivo

Un recorrido documentado y repetido en el stack real demuestra que el job resuelve una reserva a medias, que la credencial protege las rutas internas y que el gateway no las expone.

## Alcance

Levantar `docker compose up -d` (perfil `backend`) con `PAGOS_CREDENCIAL_ACADEMICO` definida y `app.reservas.minutos-para-retomar` y el cron en valores cortos (variables de entorno temporales). Para cada escenario, dejar el dato de partida directamente en `db.reservas` / `db.franjas` de MongoDB y observar el resultado:

1. **Reserva a medias con saldo** (franja `RESERVADA`, reserva `PENDIENTE_PAGO` antigua, estudiante con horas) → tras una pasada, `CONFIRMADA` y el saldo bajó **una** hora.
2. **Misma reserva, otra pasada** → no se descuenta de nuevo.
3. **Reserva a medias sin horas** → `CANCELADA` y la franja `LIBRE`.
3b. **Reserva a medias vieja** (más de `minutos-para-deshacer`, con la hora ya descontada) → `CANCELADA`, la franja `LIBRE` y el saldo recupera la hora; no se confirma.
4. **Franja liberada por fuera** → reserva `CANCELADA` y, si la hora se había descontado, devuelta.
5. **Pagos detenido** (`docker compose stop pagos`) durante **más pasadas que `maximo-intentos`** → la reserva sigue `PENDIENTE_PAGO`, `intentosRetomar` queda en 0 y **no** se marca para revisión; al levantar Pagos de nuevo, la reserva se resuelve sola.
6. **Credencial equivocada** en Académico → Pagos responde `401 ERR_PAG_19`, la reserva queda pendiente sin sumar intentos (nunca se confirma) y hay un `ERROR` en el log de cada pasada; al corregir la credencial, se resuelve sola.
7. **Sin ningún estudiante conectado**: ninguna de las pasadas necesita un token.
8. **Rutas internas desde afuera**: `POST http://localhost:8080/internal/v1/billeteras-horas/consumos` (por el gateway) → `404`; directo a Pagos sin `X-Service-Key` → `401 ERR_PAG_19`; con un JWT válido pero sin credencial → `401`.
9. **Una reserva `NO_ASISTIO` o `CONFIRMADA`** presente en la base → el job no la toca.
10. Anotar el resultado de cada escenario en las **Notas de implementación** del README (qué quedó distinto o más preciso), y borrar los datos temporales.

Si un escenario falla, se corrige **en la UT que corresponda** (no acá) y se repite el recorrido completo.

Archivos esperados (crear/tocar):

- `docs/UnidadesTrabajo/CU44 - Orquestar clases y horas/UT-BACKEND/README.md` (solo la sección de notas)

## Fuera de alcance (no tocar)

- Cambios de código en cualquier servicio.
- Probar cancelaciones o reprogramaciones (CU15).
- Pruebas de carga o de varias réplicas.
- Dejar datos de prueba en las bases.

## Resultado esperado

Todos los escenarios dan el resultado esperado en el stack real, y el README guarda lo aprendido.

## Validación

1. Todos los escenarios (1 a 9, incluido el 3b) con su resultado observado, anotados.
2. `./mvnw test` pasa en `pagos`, `academico` y `gateway`.
3. Las bases quedan sin datos de prueba.
