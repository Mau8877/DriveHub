# Errores — Académico (`ACA`)

Códigos del microservicio Académico (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `AcademicoErrorCodes` (`common/exception/`).

## `ERR_ACA_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Al crear un horario del instructor, la hora de fin no es posterior a la
  de inicio (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.crear()`

## `ERR_ACA_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Al crear un horario, la duración de la clase está fuera del rango permitido
  (15 a 240 minutos) o el tramo entre la hora de inicio y la de fin no es un múltiplo exacto de
  ella, de modo que no se podrían armar franjas completas (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.crear()`

## `ERR_ACA_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Al crear un horario, "vigente desde" es anterior a hoy, o "vigente hasta" es
  anterior a "vigente desde" (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.crear()`

## `ERR_ACA_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El horario que se intenta crear o reanudar se cruza con otro horario activo
  del mismo instructor: algún día de la semana en común, horas que se solapan y vigencias que se
  solapan. Que una ventana termine a la hora en que empieza la otra no cuenta como cruce (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.crear()` y `PlantillaHorarioService.reanudar()`

## `ERR_ACA_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El horario que se quiere pausar, reanudar o desactivar no existe o no es del instructor que lo
  pide. Se responde igual en ambos casos para no revelar que el horario de otro existe (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.pausar()`, `PlantillaHorarioService.reanudar()` y
  `PlantillaHorarioService.desactivar()`

## `ERR_ACA_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El horario que se quiere desactivar ya estaba inactivo. Desactivar es
  definitivo: un horario no se reactiva, se crea otro. Se puede desactivar un horario activo
  o pausado (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.desactivar()`

## `ERR_ACA_07`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La franja que se quiere bloquear o desbloquear no existe o no es del
  instructor que lo pide. Se responde igual en ambos casos (CU12).
- **Dónde se lanza:** `FranjaService.bloquear()` y `FranjaService.desbloquear()`

## `ERR_ACA_08`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso bloquear una franja que no está `LIBRE`: está reservada, ya estaba
  bloqueada, o cambió de estado justo antes (CU12). Una franja reservada solo se libera con la
  cancelación de la reserva.
- **Dónde se lanza:** `FranjaService.bloquear()`

## `ERR_ACA_09`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso desbloquear una franja que no está `BLOQUEADA` (CU12).
- **Dónde se lanza:** `FranjaService.desbloquear()`

## `ERR_ACA_10`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso bloquear o desbloquear una franja que ya empezó o ya pasó, o
  bloquear o desbloquear un día completo cuya fecha ya pasó (CU12).
- **Dónde se lanza:** `FranjaService.bloquear()`, `FranjaService.desbloquear()`,
  `FranjaService.bloquearDia()` y `FranjaService.desbloquearDia()`

## `ERR_ACA_11`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso pausar un horario que no está activo (ya estaba pausado o está
  inactivo) (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.pausar()`

## `ERR_ACA_12`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se quiso reanudar un horario que no está pausado (está activo o inactivo)
  (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.reanudar()`

## `ERR_ACA_13`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Al consultar las franjas por rango, la fecha inicial es posterior a la final,
  el rango abarca más de 7 días, o se indicó solo una de las dos fechas (CU12).
- **Dónde se lanza:** `FranjaService.listar()`

## `ERR_ACA_14`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La pregunta pedida no existe en la escuela de quien la pide. Una pregunta de otra
  escuela, o una eliminada lógicamente, se responde igual, para no revelar qué preguntas existen (CU19).
  Un código que no es un UUID responde `ERR_SYS_01`, no este.
- **Dónde se lanza:** `PreguntaService.buscar()`, y a través de él `obtener()`, `editar()` y `cambiarActiva()`

## `ERR_ACA_15`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se pidió activar una pregunta que ya estaba activa, o desactivar una que ya estaba
  desactivada (CU19). También se responde si dos peticiones iguales llegan a la vez y la segunda no
  cambia nada.
- **Dónde se lanza:** `PreguntaService.cambiarActiva()`

## `ERR_ACA_16`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`) o la persona
  (`sub`) no es un código válido, así que no hay escuela sobre la cual operar (CU19). Un Administrador de
  escuela siempre los trae.
- **Dónde se lanza:** `PreguntaController.escuelaDe()` y `PreguntaController.autorDe()`

## `ERR_ACA_17`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** Un servicio del que depende la operación no responde o responde algo inesperado: Pagos
  (saldo, descontar o devolver una hora) o Identidad (nombre de los instructores). Se puede reintentar más tarde
  (CU14). Al confirmar varias reservas viaja como motivo de la franja afectada, no como respuesta de toda la
  petición, y esa franja queda libre.
- **Dónde se lanza:** `PagosClient` (`saldo()`, `descontar()`, `devolver()`) e `IdentidadClient`
  (`nombresDeInstructores()`)

## `ERR_ACA_18`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Al reservar, la franja ya no está libre (otro estudiante la reservó antes, el instructor la
  bloqueó o la quitó), no existe o no es de la escuela del estudiante. No se distingue entre esos casos para no
  revelar qué franjas hay en otras escuelas (CU14). Viaja como motivo de esa franja dentro de `rechazadas`; las
  demás franjas de la misma petición siguen su curso.
- **Dónde se lanza:** `ReservaService.procesar()`

## `ERR_ACA_19`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Al reservar, Pagos informa que el estudiante no tiene horas suficientes (`ERR_PAG_09`),
  por ejemplo porque las gastó en otras franjas de la misma petición o en otro dispositivo (CU14). La franja
  queda libre y no se descuenta nada. Viaja como motivo de esa franja dentro de `rechazadas`.
- **Dónde se lanza:** `ReservaService.procesar()`

## `ERR_ACA_20`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Al reservar, el estudiante ya tiene otra clase reservada a la misma fecha y hora de inicio,
  aunque sea con otro instructor (CU14). Viaja como motivo de esa franja dentro de `rechazadas`.
- **Dónde se lanza:** `ReservaService.procesar()`
