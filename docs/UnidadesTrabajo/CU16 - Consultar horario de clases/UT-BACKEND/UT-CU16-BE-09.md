# UT-CU16-BE-09 — Prueba de punta a punta y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-09` |
| **Nombre** | Prueba de punta a punta y documentación |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` + `pagos` + `identidad` + `gateway` (solo se prueba, no se cambia código) |
| **Depende de** | `UT-CU16-BE-08` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Seguir el estilo de [`UT-CU15-BE-10`](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-BACKEND/UT-CU15-BE-10.md): stack real con Docker Compose, datos temporales con marca propia, y limpiar al terminar. Leer antes las notas de CU15 sobre cómo correr los tests contra el Mongo del compose.

## Solicitud de negocio

Confirmar que la agenda y la cancelación del instructor funcionan juntas contra Pagos, Identidad y Mongo reales, también cuando algo se corta.

## Objetivo

Un recorrido documentado en el stack real demuestra los criterios de aceptación de CU16 (backend) y que el proceso programado de CU44 termina una cancelación del instructor dejando la franja bloqueada.

## Alcance

Levantar el stack (Postgres, Mongo, Identidad, Escuela, Pagos, Académico, gateway) con `PAGOS_CREDENCIAL_ACADEMICO` definida. Con el token de un instructor real (`instructorexample@gmail.com` de `escuelaexample`) y un estudiante con horas, dejar clases de partida en `db.reservas` / `db.franjas` y en la billetera de Pagos, y observar:

1. **Agenda de un día:** clases ordenadas por hora con el nombre del estudiante y su estado; sin `PENDIENTE_PAGO` ni clases de otro instructor; día sin clases → lista vacía con `meta`.
2. **Estados visibles:** una `CONFIRMADA` terminada trae `terminada: true` y `cancelable: false`; una futura trae `cancelable: true`.
3. **Cancelar a más de 12 horas:** `CANCELADA`, franja `BLOQUEADA` (sin `reservaId`), saldo del estudiante +1, `DEVUELTA`; aparece en el «Historial» del estudiante como `CANCELADA`.
4. **Cancelar a menos de 12 horas:** igual que 3 (la hora **sí** se devuelve), y la franja `BLOQUEADA`.
5. **Pagos detenido:** `200`, `EN_PROCESO`, la reserva en `CANCELACION_PENDIENTE` con `canceladaPorInstructor`; al levantar Pagos el proceso programado la deja `CANCELADA`, franja `BLOQUEADA` y la hora devuelta **una sola vez**.
6. **Cancelar dos veces:** la segunda es `409 ERR_ACA_36` y el saldo no cambia. Cancelar una clase ya empezada o `COMPLETADA`: `409 ERR_ACA_36`.
7. **Otro instructor** intenta cancelar la clase: `404 ERR_ACA_35`. Sin sesión: `401`. Con rol de estudiante: `403 ERR_SYS_05`. `fecha` inválida: `400 ERR_SYS_01`.
8. **Estudiante e instructor a la vez** sobre la misma clase: uno gana y el otro recibe `409`; el saldo sube una sola vez.
9. **Identidad detenido:** la agenda responde `503 ERR_ACA_17`; cancelar responde `200` con el estudiante como `Estudiante`.
10. Anotar el resultado de cada escenario en las **Notas de implementación** del [README](./README.md) (qué quedó distinto o más preciso) y borrar los datos temporales.

Documentación, en el mismo cambio:

- `docs/CasosDeUso/CU16 - Consultar horario de clases.md`: **Estado** a `Hecho` cuando todas las UT (backend y móvil) estén terminadas.
- `docs/UnidadesTrabajo/CU15 - Cancelar o reprogramar clase/UT-BACKEND/README.md`: en «Follow-ups», marcar la cancelación del instructor como hecha por CU16, y en `CU15 - Cancelar o reprogramar clase.md` apuntar a [CU16](../../../CasosDeUso/CU16%20-%20Consultar%20horario%20de%20clases.md) en «Fuera de alcance».
- `docs/DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md`: anotar que el instructor ya cancela (con la hora siempre devuelta) y quitar la mención de «el instructor llega con CU16».
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: comprobar que la UT 01 dejó `canceladaPorInstructor`.

Si un escenario falla, se corrige **en la UT que corresponda** (no acá) y se repite el recorrido completo.

Archivos esperados (crear/tocar):

- `docs/UnidadesTrabajo/CU16 - Consultar horario de clases/UT-BACKEND/README.md` (solo la sección de notas y el estado)
- `docs/UnidadesTrabajo/CU15 - Cancelar o reprogramar clase/UT-BACKEND/README.md`, `docs/CasosDeUso/CU15 - Cancelar o reprogramar clase.md`
- `docs/DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md`

## Fuera de alcance (no tocar)

- Cambios de código en cualquier servicio.
- La app móvil (su prueba manual está en [UT-MOBILE](../UT-MOBILE/README.md)).
- Pruebas de carga o de varias réplicas.
- Dejar datos de prueba en las bases.

## Resultado esperado

Todos los escenarios dan el resultado esperado en el stack real y la documentación de CU15 y CU16 queda al día.

## Validación

1. Todos los escenarios (1 a 9) con su resultado observado, anotados.
2. `./mvnw test` pasa en `academico`.
3. Las bases quedan sin datos de prueba.
