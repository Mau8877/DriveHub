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
- **Significado:** La pasarela de pago (Stripe) no pudo crear la sesión de cobro (CU26) ni la devolución de un
  reembolso o una compensación (CU31): no responde, rechazó el pedido, respondió algo incompleto, o el servicio no
  tiene la clave configurada. No se cobró ni se devolvió nada; en un reembolso las horas apartadas vuelven al
  estudiante. Se puede reintentar más tarde.
- **Dónde se lanza:** `StripePasarela.crearSesionCobro()`, `StripePasarela.reembolsar()` y
  `ReversionService.solicitarReembolso()` (cuando el pago confirmado no tiene guardado el cobro de la pasarela)

## `ERR_PAG_07`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El pago pedido no existe o es de otra persona (CU26, CU28 y CU31). Un pago ajeno y un código inexistente responden igual: no se distinguen.
- **Dónde se lanza:** `PagoService.estado()`, `ConsultaPagosService.detalle()` y `ReversionService.solicitarReembolso()`

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

## `ERR_PAG_10`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La compra no se puede reembolsar (CU31): el pago no está confirmado (pendiente, fallido o ya
  reembolsado) o ya tiene una devolución vigente (pedida o ejecutada). También cuando dos pedidos del mismo reembolso
  llegan a la vez y uno pierde la carrera. No se cambia nada.
- **Dónde se lanza:** `ReversionService.solicitarReembolso()`

## `ERR_PAG_11`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya se usó al menos una hora de la compra (CU31): el reembolso es de la compra entera o de nada, no
  hay reembolso parcial. No se cambia nada.
- **Dónde se lanza:** `ReversionService.solicitarReembolso()`

## `ERR_PAG_12`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El estudiante no es alumno vigente del instructor que consulta (CU29). Un alumno de otro
  instructor, de otra escuela, uno cuya asignación terminó y un código inexistente responden igual: no se
  distingue "no existe" de "no es tuyo". También se devuelve si Identidad no conoce a la persona.
- **Dónde se lanza:** `HorasAlumnosService.detalle()`

## `ERR_PAG_13`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El comprobante no existe o quien lo pide no puede verlo (CU30): no es el estudiante dueño del
  pago ni el instructor al que ese estudiante está asignado, o es de otra escuela. No se distingue "no existe" de "no es
  tuyo".
- **Dónde se lanza:** `ComprobanteDescargaService.obtener()`

## `ERR_PAG_14`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El comprobante ya está registrado pero su PDF todavía se está preparando (CU30). Se reintenta
  solo cada pocos minutos; el cliente debe volver a pedirlo más tarde.
- **Dónde se lanza:** `ComprobanteDescargaService.obtener()`

## `ERR_PAG_15`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** Un servicio del que depende la operación no respondió o respondió algo inesperado: Identidad al
  pedir el perfil del estudiante que compra un plan (CU26), Escuela o Identidad al pedir los alumnos de un
  instructor y sus nombres (CU29), y el almacén de archivos al leer el PDF de un comprobante (CU30). No se cambió nada y
  se puede reintentar más tarde.
- **Dónde se lanza:** `IdentidadClient.perfilPropio()`, `IdentidadClient.nombresDe()`,
  `EscuelaClient.alumnosDelInstructor()`, `EscuelaClient.esMiAlumno()` y `ComprobanteDescargaService.obtener()`

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
