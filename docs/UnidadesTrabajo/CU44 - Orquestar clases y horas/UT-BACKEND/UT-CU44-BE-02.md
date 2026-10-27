# UT-CU44-BE-02 — Pagos: descontar y devolver horas sin sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-02` |
| **Nombre** | Pagos: descontar y devolver horas sin sesión |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

La decisión 6 del [README](./README.md#decisiones-de-diseño). [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Leer `BilleteraService.consumir` y `devolver` completos antes de tocar nada: su comportamiento **no puede cambiar**.

## Solicitud de negocio

El job de Académico no tiene rol ni sesión, así que no puede pasar por los métodos de la billetera que exigen un rol. Necesita las mismas operaciones, con las mismas garantías de idempotencia y de saldo, sin la autorización por sesión.

## Objetivo

`BilleteraService` ofrece `consumirDeSistema` y `devolverDeSistema`, con la misma lógica y las mismas reglas que `consumir` y `devolver` pero sin `@PreAuthorize`.

## Alcance

- Extraer el cuerpo de `consumir` y de `devolver` a métodos privados (`ejecutarConsumo`, `ejecutarDevolucion`) y hacer que los públicos existentes los llamen. **Sin cambiar** firmas, anotaciones, mensajes, códigos de error ni comportamiento de los públicos actuales.
- Dos métodos públicos nuevos, ambos `@Transactional` y **sin** `@PreAuthorize`:
  - `ConsumoResultado consumirDeSistema(UUID escuelaCodigo, UUID estudianteCodigo, int horas, UUID instructorCodigo, UUID claseCodigo, String idempotencyKey)`
  - `DevolucionResultado devolverDeSistema(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey)`
- Javadoc en cada uno con el aviso (`! `): solo los llama el sistema desde un endpoint ya autenticado por credencial de servicio; **nunca** desde un endpoint de usuario.
- Tests unitarios (con los repositorios simulados, igual que los de `consumir`/`devolver`): `consumirDeSistema` descuenta y deja el movimiento; con la misma clave devuelve lo anterior sin descontar de nuevo; sin horas → `ERR_PAG_09` y no cambia nada; `devolverDeSistema` devuelve lo gastado, con la misma clave no devuelve dos veces, clase sin consumo → `ERR_PAG_16`.
- Los tests que ya existen de `consumir` y `devolver` deben seguir pasando **sin modificarlos**: es la prueba de que el refactor no cambió nada.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/service/BilleteraService.java`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/service/` (tests nuevos)

## Fuera de alcance (no tocar)

- El controller y las rutas (UT 03): esta UT no expone nada por HTTP.
- La credencial de servicio y el filtro (UT 01).
- Cambiar la política de roles de `consumir` y `devolver`.
- Reembolso (`apartarPorReembolso`, `restituirApartadas`) y acreditar: no se tocan.

## Resultado esperado

El service tiene dos puertas de entrada al mismo núcleo: la de usuarios (con rol) y la de sistema (sin rol). El comportamiento de la primera no cambió.

## Validación

1. `./mvnw test` en `pagos` pasa, incluidos los tests anteriores de `BilleteraService` sin cambios.
2. `./mvnw checkstyle:check` sin errores.
3. Confirmar en el diff que `consumir` y `devolver` conservan su `@PreAuthorize(ROLES_QUE_GASTAN)`.
