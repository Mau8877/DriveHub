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
- **Significado:** El horario que se intenta crear se cruza con otro horario activo del mismo
  instructor: mismo día de la semana, horas que se solapan y vigencias que se solapan. Que una
  ventana termine a la hora en que empieza la otra no cuenta como cruce (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.crear()`

## `ERR_ACA_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El horario que se quiere desactivar no existe o no es del instructor que lo
  pide. Se responde igual en ambos casos para no revelar que el horario de otro existe (CU12).
- **Dónde se lanza:** `PlantillaHorarioService.desactivar()`

## `ERR_ACA_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El horario que se quiere desactivar ya estaba inactivo. Desactivar es
  definitivo: un horario no se reactiva, se crea otro (CU12).
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
- **Significado:** Se quiso bloquear o desbloquear una franja que ya empezó o ya pasó (CU12).
- **Dónde se lanza:** `FranjaService.bloquear()` y `FranjaService.desbloquear()`
