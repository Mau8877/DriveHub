# UT-CU15-BE-10 — Prueba de punta a punta y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-10` |
| **Nombre** | Prueba de punta a punta y documentación |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` + `pagos` + `gateway` (solo se prueba, no se cambia código) |
| **Depende de** | `UT-CU15-BE-09` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Seguir el estilo de la prueba de punta a punta de CU44 ([`UT-CU44-BE-12`](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/UT-CU44-BE-12.md)): stack real con Docker Compose, datos temporales con marca propia, y limpiar al terminar.

## Solicitud de negocio

Confirmar que cancelar y reprogramar funcionan juntos contra Pagos y Mongo reales, también cuando algo se corta.

## Objetivo

Un recorrido documentado en el stack real demuestra los criterios de aceptación de CU15, que el proceso programado de CU44 retoma lo que queda a medias sin cambios, y que las notas de CU44 y CU15 reflejan lo construido.

## Alcance

Levantar el stack (Postgres, Mongo, Identidad, Escuela, Pagos, Académico, gateway) con `PAGOS_CREDENCIAL_ACADEMICO` definida. Para cada escenario, dejar el dato de partida en `db.reservas` / `db.franjas` y en la billetera de Pagos, usando el token de un estudiante real de `escuelaexample` o datos temporales, y observar:

1. **Cancelar a tiempo** (clase a más de 12 horas): `CANCELADA`, franja `LIBRE`, saldo +1, `horaDevuelta: DEVUELTA`; aparece en el historial.
2. **Cancelar tarde** (menos de 12 horas): `CANCELADA`, franja `LIBRE`, saldo sin cambio, `PERDIDA`, y ningún movimiento `DEVOLUCION` en Pagos.
3. **Pagos detenido al cancelar a tiempo:** `200`, `EN_PROCESO`, la reserva en `CANCELACION_PENDIENTE` fuera de «Mis clases»; al levantar Pagos el proceso programado la deja `CANCELADA` con la hora devuelta **una sola vez**, sin que haya sumado intentos.
4. **Cancelar dos veces** (repetir la petición): la segunda es `409 ERR_ACA_33` y el saldo no cambia.
5. **Reprogramar** a una franja libre: la clase queda `CONFIRMADA` en la nueva, la anterior `LIBRE`, saldo sin cambio; reprogramar de nuevo funciona.
6. **Reprogramar con menos de 12 horas:** `409 ERR_ACA_34`. Reprogramar a una franja ocupada: `409 ERR_ACA_18` con la clase intacta. A una franja donde ya tiene otra clase: `409 ERR_ACA_20`.
7. **Reprogramación interrumpida:** dejar una reserva en `REPROGRAMACION_PENDIENTE` con `franjaDestinoId` y la destino (a) ya apartada por ella y (b) todavía libre; tras una pasada del proceso programado, (a) y (b) quedan `CONFIRMADA` en la destino con la anterior `LIBRE`; con la destino tomada por otra reserva queda `CONFIRMADA` en la original.
8. **Otro estudiante** intenta cancelar o reprogramar la clase: `404 ERR_ACA_32`. Sin sesión: `401`. Con rol de instructor: `403 ERR_SYS_05`.
9. **El proceso programado no toca** una reserva recién pasada a `CANCELACION_PENDIENTE` hasta que pasan `minutos-para-retomar` (la antigüedad se mide por `updatedAt`).
10. Anotar el resultado de cada escenario en las **Notas de implementación** del [README](./README.md) (qué quedó distinto o más preciso) y borrar los datos temporales.

Documentación, en el mismo cambio:

- `docs/CasosDeUso/CU15 - Cancelar o reprogramar clase.md`: **Estado** a `Hecho` cuando todas las UT (backend y móvil) estén terminadas.
- README de [CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md): marcar las UT 17 y 18 como hechas por las UT 05 y 06 de CU15, y anotar que `CompensadorDeReserva` se generalizó (UT 04) y que la antigüedad de una reserva a medias se mide por `updatedAt`.
- `docs/DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md`: la fila «Devolver la hora al cancelar la clase (CU15)» pasa de «Pendiente» a hecha, con el plazo de 12 horas.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: comprobar que la UT 01 dejó los estados y campos.

> **Estado (2026-11-01):** los escenarios 3 (con el proceso programado), 7 y 9 se probaron con el stack real; ver las notas del [README](./README.md). Los demás (1, 2, 4, 5, 6 y 8) por HTTP necesitan el JWT de un estudiante de `escuelaexample` y siguen pendientes, igual que pasar el CU15 a `Hecho`.

Si un escenario falla, se corrige **en la UT que corresponda** (no acá) y se repite el recorrido completo.

Archivos esperados (crear/tocar):

- `docs/UnidadesTrabajo/CU15 - Cancelar o reprogramar clase/UT-BACKEND/README.md` (solo la sección de notas)
- `docs/UnidadesTrabajo/CU44 - Orquestar clases y horas/UT-BACKEND/README.md`
- `docs/DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md`

## Fuera de alcance (no tocar)

- Cambios de código en cualquier servicio.
- Cancelaciones por el instructor (CU16).
- Pruebas de carga o de varias réplicas.
- Dejar datos de prueba en las bases.

## Resultado esperado

Todos los escenarios dan el resultado esperado en el stack real y la documentación de CU15 y CU44 queda al día.

## Validación

1. Todos los escenarios (1 a 9) con su resultado observado, anotados.
2. `./mvnw test` pasa en `academico`, `pagos` y `gateway`.
3. Las bases quedan sin datos de prueba.
