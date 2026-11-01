# UT-CU15-BE-08 — `ReservaService`: reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-08` |
| **Nombre** | `ReservaService`: reprogramar |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-02`, `UT-CU15-BE-03`, `UT-CU15-BE-06` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Decisiones 6 y 8 del [README](./README.md) y las reglas de franja de [CU14](../../../CasosDeUso/CU14%20-%20Reservar%20horario.md).

## Solicitud de negocio

El estudiante que no puede ir a una clase pero quiere conservar su hora la cambia a otra franja libre, sin pagar de nuevo.

## Objetivo

`ReservaService.reprogramar` valida la clase y la franja elegida, aparta la franja nueva y deja la clase en ella, sin tocar las horas, con el mismo comportamiento ante cortes que el manejador.

## Alcance

- `ReservaService.reprogramar(UUID estudianteCodigo, UUID escuelaCodigo, String bearer, UUID codigoReserva, UUID franjaCodigo)` devuelve `ReservaResponse` (el DTO de CU14, ver UT 09 para su campo nuevo), con `@PreAuthorize("hasRole('ESTUDIANTE')")`:
  1. Busca la reserva del estudiante; no existe o es ajena → `NotFoundException` `ERR_ACA_32`.
  2. Si no está `CONFIRMADA` o ya empezó → `ConflictException` `ERR_ACA_33`. Si no está a tiempo (`PlazoDeClase.aTiempo`) → `ConflictException` `ERR_ACA_34`.
  3. Si `franjaCodigo` es la franja actual de la reserva → `ValidationException` `ERR_SYS_01` («Elige una franja distinta a la actual»).
  4. Valida la franja destino **con las mismas reglas y los mismos códigos que `reservar`** (existe y es de la escuela del estudiante, `LIBRE`, no empezó): `ERR_ACA_18` / `ERR_ACA_10`. Si la verificación hoy es privada de `reservar`, se extrae a un método reutilizable del mismo paquete **en esta UT**; no se copia.
  5. Solape: `existeSolape(estudiante, fecha destino, hora destino, codigoReserva)` (UT 02; la clase que se mueve no cuenta) → `ConflictException` `ERR_ACA_20`.
  6. `iniciarReprogramacion` (UT 02); vacío (otra petición ganó) → `ERR_ACA_33`.
  7. `apartar` la franja destino con el `_id` de la reserva. Si ya no está libre → `revertirReprogramacion` y `ERR_ACA_18`, con la clase intacta en su franja original.
  8. Termina con `ManejadorDeReprogramacion.terminar` (UT 06): libera la anterior y mueve la reserva.
  9. Devuelve la reserva ya en la franja nueva, con el nombre del instructor (Identidad, como en CU14; si Identidad no responde → `ServicioNoDisponibleException` `ERR_ACA_17`, y la reprogramación **ya hecha** no se deshace).
  - Sin llamadas a Pagos.
  - Logs `INFO` con los códigos de reserva y de las dos franjas.
- Tests de servicio (repositorios, manejador y cliente de Identidad simulados, `Clock` fijo): camino feliz mueve la clase y no llama a Pagos; destino igual a la actual → `ERR_SYS_01`; fuera de plazo → `ERR_ACA_34`; clase ajena → `ERR_ACA_32`; no `CONFIRMADA` o empezada → `ERR_ACA_33`; destino ocupada → `ERR_ACA_18` y la reserva vuelve a `CONFIRMADA`; destino ya empezada → `ERR_ACA_10`; solape con otra clase → `ERR_ACA_20` y la propia no cuenta; reprogramar dos veces seguidas funciona (sin límite); un corte entre apartar y mover deja `REPROGRAMACION_PENDIENTE` que el manejador termina.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/ReservaServiceTest.java`

## Fuera de alcance (no tocar)

- El DTO nuevo de entrada, el controller y la ruta (UT 09).
- Un límite de reprogramaciones: no hay (decisión del CU).
- Reprogramar por el instructor (CU16) o con anticipación distinta a la de cancelar.
- Tocar la billetera o `PagosClient`.

## Resultado esperado

Una clase se mueve a otra franja libre del mismo estudiante, con las mismas horas, y ante cualquier fallo queda en la franja original o en la nueva, nunca en ninguna.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Los tests de `reservar` de CU14 siguen en verde tras extraer la verificación de la franja.
