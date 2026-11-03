# UT-CU15-BE-06 — `ManejadorDeReprogramacion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-06` |
| **Nombre** | `ManejadorDeReprogramacion` |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-02` |

## Antes de implementar

La guía «Contrato para CU15» del [README de CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md), el Javadoc de `ManejadorDeOperacion` y las decisiones 8 y 10 de este [README](./README.md). [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Si cambiar una clase de franja se corta a mitad, el estudiante no puede quedarse sin clase ni con dos franjas apartadas: el sistema termina el cambio o lo deshace solo.

## Objetivo

Un bean `ManejadorDeReprogramacion` termina o deshace las reservas en `REPROGRAMACION_PENDIENTE`, y expone la secuencia de pasos que el servicio también usa en vivo, para que haya una sola implementación.

## Alcance

- `ManejadorDeReprogramacion` (`features/reservas/service/`, `@Component`, `@RequiredArgsConstructor`) implementa `ManejadorDeOperacion`:
  - `estados()` devuelve `Set.of(EstadoReserva.REPROGRAMACION_PENDIENTE)`.
  - `resolver(reserva, cliente)` (el cliente de Pagos **no se usa**: reprogramar no toca las horas) lee la franja destino (`franjaDestinoId`) y la anterior (`franjaId`) y:
    1. Si la franja destino está `RESERVADA` por esta reserva → **termina**: `liberar` la franja anterior (solo si sigue siendo de esta reserva, nunca la de otra) y `completarReprogramacion` (UT 02) con los datos de la destino → `CONFIRMADA`.
    2. Si la destino está `LIBRE` → intenta `apartar` otra vez (misma escuela, futura); si lo logra, sigue como en 1; si ya no se puede (otro estudiante la tomó, ya empezó) → **deshace**: `revertirReprogramacion` → `CONFIRMADA` en la franja original, que sigue siendo suya.
    3. Si la destino no existe o es de otra reserva → deshace como en 2.
    - Devuelve `CONFIRMADA` cuando termina o deshace con la clase confirmada, y `PENDIENTE` (cuenta intento) si no pudo avanzar por una causa propia (por ejemplo, la franja anterior no existe).
  - Un método público `ResultadoOrquestacion terminar(Reserva reserva)` con esa secuencia, que `resolver` llama y que el servicio de la UT 08 reutiliza al final de su flujo en vivo. Los pasos del servicio y del manejador no se escriben dos veces.
  - Cada paso es repetible: `liberar` y las operaciones atómicas del repositorio son condicionales, así que correr el manejador dos veces (por ejemplo, el estudiante y el proceso programado a la vez) no libera de más ni deja a la reserva en dos franjas.
  - Registra `INFO` al terminar y `WARN` al deshacer o quedar pendiente, con los códigos de reserva y de franja.
- Test unitario `ManejadorDeReprogramacionTest` (repositorios simulados): declara solo `REPROGRAMACION_PENDIENTE`; destino ya apartada por la reserva → libera la anterior y mueve; destino libre y apartable → la aparta y mueve; destino libre pero tomada por otra → revierte y la clase queda en la original; destino ya de otra reserva → revierte; no libera una franja anterior que pasó a ser de otra reserva; repetido no libera dos veces; sin tocar Pagos.
- Test en `OrquestadorClasesTest`: el manejador convive con los otros dos sin conflicto de estados.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeReprogramacion.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/ManejadorDeReprogramacionTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/OrquestadorClasesTest.java`

## Fuera de alcance (no tocar)

- Iniciar una reprogramación y validar la franja elegida, el plazo y el solape (UT 08).
- `OrquestadorClases` y `OrquestadorClasesJob`.
- Llamar a Pagos: reprogramar no mueve horas.

## Resultado esperado

Una reprogramación interrumpida termina en la franja nueva o vuelve a la original, nunca en una situación intermedia.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. El proceso programado retoma una reserva en `REPROGRAMACION_PENDIENTE` sin cambiar el job (se comprueba en la UT 10 con el stack real).
