# Errores — Pagos (`PAG`)

Códigos del microservicio Pagos (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `PagosErrorCodes` (`common/exception/`).

## `ERR_PAG_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe otro plan de horas de la misma escuela con ese nombre (CU25). Se compara sin
  distinguir mayúsculas ni espacios de los extremos y cuentan también los planes desactivados; el mismo nombre
  en otra escuela sí se acepta. También se devuelve si dos altas con el mismo nombre llegan a la vez y la base
  rechaza la segunda (índice único `uq_plan_pago_escuela_nombre`).
- **Dónde se lanza:** `PlanPagoService.crear()` y `PlanPagoService.editar()`

## `ERR_PAG_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El plan pedido no existe en la escuela de quien consulta (CU25). Un plan de otra escuela y
  un código inexistente responden igual: no se distinguen.
- **Dónde se lanza:** `PlanPagoService.obtener()`, `editar()`, `desactivar()` y `activar()`

## `ERR_PAG_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se pidió desactivar un plan que ya está desactivado, o reactivar uno que ya está activo
  (CU25). No se cambia nada.
- **Dónde se lanza:** `PlanPagoService.desactivar()` y `PlanPagoService.activar()`

## `ERR_PAG_04`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`), así que no hay escuela
  sobre la cual operar (CU25). Un Administrador de escuela siempre la trae.
- **Dónde se lanza:** `PlanPagoController.escuelaDe()`

## `ERR_PAG_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El plan que el estudiante quiere comprar no existe, está desactivado o es de otra escuela (CU26). Una de otra escuela y un código inexistente responden igual: no se distinguen. No se creó ningún pago.
- **Dónde se lanza:** `PagoService.iniciar()`

## `ERR_PAG_06`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** La pasarela de pago (Stripe) no pudo crear la sesión de cobro (CU26): no responde, rechazó el
  pedido, respondió algo incompleto, o el servicio no tiene la clave configurada. No se cobró nada y se puede
  reintentar más tarde.
- **Dónde se lanza:** `StripePasarela.crearSesionCobro()`

## `ERR_PAG_07`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El pago pedido no existe o es de otra persona (CU26 y CU28). Un pago ajeno y un código inexistente responden igual: no se distinguen.
- **Dónde se lanza:** `PagoService.estado()` y `ConsultaPagosService.detalle()`

## `ERR_PAG_08`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Un aviso que dice venir de la pasarela de pago no se pudo verificar (CU27): le falta la firma, la
  firma no es válida, el cuerpo fue alterado, la hora del aviso está fuera de la tolerancia, o el servicio no tiene
  configurado el secreto con que se verifican. No se cambia nada.
- **Dónde se lanza:** `StripePasarela.verificarAviso()`

## `ERR_PAG_09`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso gastar más horas de las que tiene la billetera del estudiante, o el estudiante todavía no
  tiene billetera (CU27). No se descuenta nada.
- **Dónde se lanza:** `BilleteraService.consumir()`

## `ERR_PAG_15`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** Un servicio del que depende la operación no respondió o respondió algo inesperado: hoy,
  Identidad al pedir el perfil del estudiante que compra un plan (CU26). No se creó ningún pago y se puede
  reintentar más tarde.
- **Dónde se lanza:** `IdentidadClient.perfilPropio()`

## `ERR_PAG_16`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** No hay horas para devolver de esa clase (CU27): la clase nunca gastó horas de esa billetera, o ya
  se le devolvió todo lo que gastó.
- **Dónde se lanza:** `BilleteraService.devolver()`

## `ERR_PAG_17`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se está procesando una compra con esa misma clave y su sesión de cobro todavía no quedó guardada (CU26): el estudiante debe esperar unos segundos y reintentar con la misma clave.
- **Dónde se lanza:** `PagoService.crearSesion()`

## `ERR_PAG_18`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La clave de compra (`Idempotency-Key`) ya se usó para otra compra: de otro estudiante, de otra escuela o de otro plan (CU26). Cada intento de compra lleva su propia clave.
- **Dónde se lanza:** `PagoService.repetir()`
