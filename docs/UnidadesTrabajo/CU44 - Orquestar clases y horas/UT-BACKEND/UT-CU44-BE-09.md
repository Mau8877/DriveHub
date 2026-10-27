# UT-CU44-BE-09 — Académico: `OrquestadorClases`, manejadores y `ManejadorDeReserva`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-09` |
| **Nombre** | Académico: `OrquestadorClases`, manejadores y `ManejadorDeReserva` |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-08`, `UT-CU44-BE-14` |

## Antes de implementar

Las decisiones 2, 5, 7, 10, 11, 13, 14, 15 y 16 del [README](./README.md#decisiones-de-diseño). **Leer completo `ReservaService.retomar` y `cancelar`**: la lógica es la misma, con otro cliente. [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) (idempotencia), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una reserva que quedó a medias debe terminar bien por su cuenta: confirmada si es reciente y la hora se pudo descontar, o cancelada con la franja libre y la hora devuelta si ya es vieja o no se pudo. Y la estructura que lo hace debe poder recibir, más adelante, cancelar y reprogramar sin reescribirse.

## Objetivo

`OrquestadorClases` coordina la saga con **manejadores enchufables**; `ManejadorDeReserva` resuelve las reservas `PENDIENTE_PAGO` (completar o deshacer según su edad) y `deshacerReserva` es la operación pública y reutilizable de deshacer. Ninguna descuenta ni devuelve dos veces.

## Alcance

- Propiedad `app.orquestador.minutos-para-deshacer` (por defecto `60`) en una `OrquestadorProperties` (`@ConfigurationProperties(prefix = "app.orquestador")`, `record`, en `config/`, registrada como `ReservasProperties`). **La crea esta UT**; las UT 10 y 11 le suman sus propiedades.
- Enum `ResultadoOrquestacion { CONFIRMADA, CANCELADA, PENDIENTE, PAGOS_NO_RESPONDE, EN_REVISION }` en `features/reservas/service/`. `PENDIENTE` es «no se pudo resolver por una causa propia» (cuenta como intento, UT 10); `PAGOS_NO_RESPONDE` es «Pagos no respondió o rechazó la credencial» (**no** cuenta como intento); `EN_REVISION` lo usa la UT 10.
- **`ManejadorDeOperacion` (interfaz)** en `features/reservas/service/` — el **punto de enchufe** (decisión 15):
  - `Set<EstadoReserva> estados()`: los estados transitorios que este manejador atiende.
  - `ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente)`: resuelve **una** reserva que está en alguno de esos estados con el cliente de Pagos recibido (el de servicio en el job, el del estudiante en el flujo en vivo).
  - Cada manejador es un bean de Spring; el orquestador los recibe como `List<ManejadorDeOperacion>` y falla al arrancar si **dos manejadores declaran el mismo estado**.
  - Su Javadoc lo completa la UT 15.
- **`ManejadorDeReserva`** (`@Component`): `estados()` es `{PENDIENTE_PAGO}` y `resolver` hace el flujo S1:
  1. Lee la franja de la reserva (`franjaRepository.findById(reserva.getFranjaId())`). La escuela sale de `franja.getEscuelaCodigo()`.
  2. Si la franja **ya no está** `RESERVADA` por esa reserva → `deshacerReserva(reserva, "La franja ya no estaba apartada", cliente)`, sin importar la edad.
  3. Si la reserva lleva **`minutos-para-deshacer` o más** desde su creación (con el `Clock` inyectado) → `deshacerReserva(reserva, "Reserva a medias vencida", cliente)`, **sin intentar completarla**.
  4. Si es más reciente: repite `cliente.descontar(...)` con la **misma** clave `"reserva-" + codigoReserva`, `claseCodigo = codigoReserva` y las horas de la reserva:
     - Éxito (ya había descontado, o descuenta ahora) → `cambiarEstado(PENDIENTE_PAGO → CONFIRMADA)`, motivo «Reserva retomada por el sistema» → `CONFIRMADA`.
     - `SinHorasException` → `deshacerReserva(reserva, "Sin horas suficientes", cliente)`.
     - `ServicioNoDisponibleException` → no toca nada → `PAGOS_NO_RESPONDE`.
  5. Si la franja no existe → `PENDIENTE` con un `WARN`; la UT 10 decide qué hacer.
- **`OrquestadorClases`** (`@Service`, `@RequiredArgsConstructor`) en `features/reservas/service/`, **sin `@PreAuthorize`** (no hay sesión):
  - `Set<EstadoReserva> estadosTransitorios()`: la unión de los `estados()` de todos los manejadores. Es lo que el repositorio (UT 08) y el job (UT 11) usan para saber qué buscar.
  - `ResultadoOrquestacion resolver(Reserva reserva, ClienteDePagos cliente)`: busca el manejador del estado de la reserva y le delega. Una reserva en un estado que **ningún manejador** atiende no se toca.
  - `ResultadoOrquestacion deshacerReserva(Reserva reserva, String motivo, ClienteDePagos cliente)` — **pública y reutilizable (la usará CU15)**. Lee la franja; llama a `cliente.devolver(...)` con `"devolucion-" + codigoReserva` (un `ERR_PAG_16` no es error); libera la franja solo si sigue `RESERVADA` por esa reserva; y pasa la reserva `PENDIENTE_PAGO → CANCELADA` con el `motivo` en su historial → `CANCELADA`. `ServicioNoDisponibleException` en la devolución → no toca nada → `PAGOS_NO_RESPONDE`. Si la franja no existe → `PENDIENTE` con un `WARN`.
  - Usa las constantes de clave de `ReservaService` (mismo paquete) y el `Clock` inyectado; no escribe su propia copia del prefijo.
  - El cliente de servicio se obtiene del contenedor (`@Qualifier("clienteDeServicio")`, UT 14) y es el que usa el job (UT 11).
  - **Comentarios para quien implemente CU15 (Leonel):** el Javadoc de la clase y el de `deshacerReserva` llevan, en el formato Better Comments de [ESTANDARES_CODIGO.md](../../../../.claude/rules/ESTANDARES_CODIGO.md), qué hace CU15 con esta pieza. Texto mínimo, adaptado al código:
    - En `deshacerReserva`: `* * Esta es la operación de cancelar: CU15 la reutiliza cuando una clase se cancela. No escribir otra copia de devolver la hora.` y `* ! Pagos siempre devuelve lo que se le pide: las reglas de plazo de CU15 se deciden ANTES de llamar acá.`
    - En la clase: `* TODO: CU44 UT 17 y 18: agregar los manejadores de cancelación y reprogramación de CU15 cuando existan; no editar este núcleo.` y una referencia a la [guía del README](./README.md#contrato-para-cu15-cómo-se-enchufa-guía-para-leonel).
  - Cada resultado se registra con `INFO` (`reservaCodigo`, resultado); nunca datos de pago. Si Pagos rechazó la credencial, el cliente (UT 06) ya registró el `ERROR`.
- Tests unitarios (clientes y repositorios simulados): descuento ya hecho y reserva reciente → confirmada con la misma clave; sin descuento y con saldo → descuenta y confirma; sin horas → franja libre y cancelada; Pagos caído al descontar o al devolver → `PAGOS_NO_RESPONDE` y nada cambia; franja liberada → devuelve la hora y cancela; **reserva con 59 minutos se completa y con 60 se deshace** (frontera); reserva vieja con la hora ya descontada → se devuelve y se cancela, no se confirma; devolución con `ERR_PAG_16` no falla; franja inexistente → `PENDIENTE`; una reserva que ya no está `PENDIENTE_PAGO` (la confirmó el estudiante a la vez) no se cambia ni se descuenta de nuevo; **`deshacerReserva` llamado dos veces devuelve y libera una sola vez**; **el orquestador nunca toca reservas `NO_ASISTIO`, `COMPLETADA` ni `CONFIRMADA`** (ningún manejador las declara); **dos manejadores con el mismo estado hacen fallar el arranque**; `estadosTransitorios()` es la unión de los estados de los manejadores; **el mismo manejador funciona con el cliente del estudiante y con el de servicio** (cambia la credencial, no el resultado).
- Test de integración con MongoDB real (patrón `assumeTrue`): una reserva `PENDIENTE_PAGO` reciente con su franja `RESERVADA` queda `CONFIRMADA` tras `resolver`, y una vieja queda `CANCELADA` con la franja `LIBRE`, con un servidor falso de Pagos.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeOperacion.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeReserva.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ResultadoOrquestacion.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/OrquestadorProperties.java` (crear)
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (unitarios e integración)

## Fuera de alcance (no tocar)

- `ReservaService`, `PagosClient`, `ReservaController`: **no se modifican ni se refactoriza para compartir código** en esta UT (lo hace la UT 16).
- Contar intentos fallidos y la revisión manual (UT 10).
- Recorrer las pendientes y programarlas (UT 11).
- Los manejadores de cancelar y reprogramar (CU15, UT 17 y 18): `deshacerReserva` y la interfaz quedan listas, pero ningún endpoint ni regla de CU15 se construye acá.
- El retomar del estudiante al consultar sus clases: sigue como está hasta la UT 16.

## Resultado esperado

Dada una reserva a medias, el orquestador la deja confirmada (si es reciente) o cancelada con la hora devuelta y la franja libre (si es vieja o no hay horas), o no la toca si Pagos no responde, y nunca descuenta ni devuelve dos veces la misma hora. Existe una interfaz de manejadores y una operación pública `deshacerReserva` que CU15 podrá reutilizar sin editar el núcleo.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el servidor falso de Pagos: dos `resolver` seguidos sobre la misma reserva descuentan **una sola vez** (el segundo ya la encuentra confirmada y no hace nada); y dos `deshacerReserva` seguidos devuelven **una sola vez**.
