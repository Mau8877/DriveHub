# Errores — Pagos (`PAG`)

Códigos del microservicio Pagos (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `PagosErrorCodes` (`common/exception/`).

## `ERR_PAG_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe otro plan de horas de la misma escuela con ese nombre (CU25a). Se compara sin
  distinguir mayúsculas ni espacios de los extremos y cuentan también los planes desactivados; el mismo nombre
  en otra escuela sí se acepta. También se devuelve si dos altas con el mismo nombre llegan a la vez y la base
  rechaza la segunda (índice único `uq_plan_pago_escuela_nombre`).
- **Dónde se lanza:** `PlanPagoService.crear()` y `PlanPagoService.editar()`

## `ERR_PAG_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El plan pedido no existe en la escuela de quien consulta (CU25a). Un plan de otra escuela y
  un código inexistente responden igual: no se distinguen.
- **Dónde se lanza:** `PlanPagoService.obtener()`, `editar()`, `desactivar()` y `activar()`

## `ERR_PAG_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se pidió desactivar un plan que ya está desactivado, o reactivar uno que ya está activo
  (CU25a). No se cambia nada.
- **Dónde se lanza:** `PlanPagoService.desactivar()` y `PlanPagoService.activar()`

## `ERR_PAG_04`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`), así que no hay escuela
  sobre la cual operar (CU25a). Un Administrador de escuela siempre la trae.
- **Dónde se lanza:** `PlanPagoController.escuelaDe()`
