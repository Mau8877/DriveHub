# UT-CU44-BE-09 — Académico: `OrquestadorClases`, completar o deshacer una reserva

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-09` |
| **Nombre** | Académico: `OrquestadorClases`, completar o deshacer una reserva |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-06`, `UT-CU44-BE-08` |

## Antes de implementar

Las decisiones 2, 5, 7, 10, 11, 13 y 14 del [README](./README.md#decisiones-de-diseño). **Leer completo `ReservaService.retomar` y `cancelar`**: la lógica es la misma, con otro cliente. [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) (idempotencia), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una reserva que quedó a medias debe terminar bien por su cuenta: confirmada si es reciente y la hora se pudo descontar, o cancelada con la franja libre y la hora devuelta si ya es vieja o no se pudo.

## Objetivo

`OrquestadorClases.retomarReserva(reserva)` **completa o deshace** una reserva `PENDIENTE_PAGO` sin sesión de estudiante, según su edad, y `OrquestadorClases.deshacerReserva(reserva, motivo)` es la operación pública y reutilizable de deshacer. Ninguna descuenta ni devuelve dos veces.

## Alcance

- Propiedad `app.orquestador.minutos-para-deshacer` (por defecto `60`) en una `OrquestadorProperties` (`@ConfigurationProperties(prefix = "app.orquestador")`, `record`, en `config/`, registrada como `ReservasProperties`). **La crea esta UT**; las UT 10 y 11 le suman sus propiedades.
- Enum `ResultadoOrquestacion { CONFIRMADA, CANCELADA, PENDIENTE, PAGOS_NO_RESPONDE, EN_REVISION }` en `features/reservas/service/`. `PENDIENTE` es «no se pudo resolver por una causa propia» (cuenta como intento, UT 10); `PAGOS_NO_RESPONDE` es «Pagos no respondió o rechazó la credencial» (**no** cuenta como intento); `EN_REVISION` lo usa la UT 10.
- `OrquestadorClases` (`@Service`, `@RequiredArgsConstructor`) en `features/reservas/service/`, **sin `@PreAuthorize`** (no hay sesión):
  - `ResultadoOrquestacion deshacerReserva(Reserva reserva, String motivo)` — **pública y reutilizable (la usará CU15)**. Lee la franja (`franjaRepository.findById`, la escuela sale de `franja.getEscuelaCodigo()`); llama a `PagosServicioClient.devolver(...)` con `"devolucion-" + codigoReserva` (un `ERR_PAG_16` no es error); libera la franja solo si sigue `RESERVADA` por esa reserva; y pasa la reserva `PENDIENTE_PAGO → CANCELADA` con el `motivo` en su historial → `CANCELADA`. `ServicioNoDisponibleException` en la devolución → no toca nada → `PAGOS_NO_RESPONDE`. Si la franja no existe (no se sabe la escuela) → `PENDIENTE` con un `WARN`.
  - `ResultadoOrquestacion retomarReserva(Reserva reserva)`:
    1. Lee la franja de la reserva.
    2. Si la franja **ya no está** `RESERVADA` por esa reserva → `deshacerReserva(reserva, "La franja ya no estaba apartada")`, sin importar la edad.
    3. Si la reserva lleva **`minutos-para-deshacer` o más** desde su creación (con el `Clock` inyectado) → `deshacerReserva(reserva, "Reserva a medias vencida")`, **sin intentar completarla**.
    4. Si es más reciente: repite `PagosServicioClient.descontar(...)` con la **misma** clave `"reserva-" + codigoReserva`, `claseCodigo = codigoReserva` y las horas de la reserva:
       - Éxito (ya había descontado, o descuenta ahora) → `cambiarEstado(PENDIENTE_PAGO → CONFIRMADA)`, motivo «Reserva retomada por el sistema» → `CONFIRMADA`.
       - `SinHorasException` → `deshacerReserva(reserva, "Sin horas suficientes")` (no hay hora que devolver: el `ERR_PAG_16` no es error).
       - `ServicioNoDisponibleException` → no toca nada → `PAGOS_NO_RESPONDE`.
    5. Si la franja no existe → `PENDIENTE` con un `WARN`; la UT 10 decide qué hacer.
  - Usa las constantes de clave de `ReservaService` (mismo paquete) y el `Clock` inyectado; no escribe su propia copia del prefijo.
  - Cada resultado se registra con `INFO` (`reservaCodigo`, resultado); nunca datos de pago. Si Pagos rechazó la credencial, el cliente (UT 06) ya registró el `ERROR`.
- Tests unitarios (clientes y repositorios simulados): descuento ya hecho y reserva reciente → confirmada con la misma clave; sin descuento y con saldo → descuenta y confirma; sin horas → franja libre y cancelada; Pagos caído al descontar o al devolver → `PAGOS_NO_RESPONDE` y nada cambia; franja liberada → devuelve la hora y cancela; **reserva con 59 minutos se completa y con 60 se deshace** (frontera); reserva vieja con la hora ya descontada → se devuelve y se cancela, no se confirma; devolución con `ERR_PAG_16` no falla; franja inexistente → `PENDIENTE`; una reserva que ya no está `PENDIENTE_PAGO` (la confirmó el estudiante a la vez) no se cambia ni se descuenta de nuevo; **`deshacerReserva` llamado dos veces devuelve y libera una sola vez**; **el orquestador nunca toca reservas `NO_ASISTIO`, `COMPLETADA` ni `CONFIRMADA`**.
- Test de integración con MongoDB real (patrón `assumeTrue`): una reserva `PENDIENTE_PAGO` reciente con su franja `RESERVADA` queda `CONFIRMADA` tras `retomarReserva`, y una vieja queda `CANCELADA` con la franja `LIBRE`, con un servidor falso de Pagos.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ResultadoOrquestacion.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/OrquestadorProperties.java` (crear)
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (unitarios e integración)

## Fuera de alcance (no tocar)

- `ReservaService`, `PagosClient`, `ReservaController`: **no se modifican ni se refactoriza para compartir código** (decisión 11).
- Contar intentos fallidos y la revisión manual (UT 10).
- Recorrer las pendientes y programarlas (UT 11).
- Cancelaciones y reprogramaciones (CU15): `deshacerReserva` queda lista para ella, pero ningún endpoint ni regla de CU15 se construye acá.
- El retomar del estudiante al consultar sus clases: sigue como está.

## Resultado esperado

Dada una reserva a medias, el orquestador la deja confirmada (si es reciente) o cancelada con la hora devuelta y la franja libre (si es vieja o no hay horas), o no la toca si Pagos no responde, y nunca descuenta ni devuelve dos veces la misma hora. Existe una operación pública `deshacerReserva` que CU15 podrá reutilizar.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el servidor falso de Pagos: dos `retomarReserva` seguidos sobre la misma reserva descuentan **una sola vez** (el segundo ya la encuentra confirmada y no hace nada); y dos `deshacerReserva` seguidos devuelven **una sola vez**.
