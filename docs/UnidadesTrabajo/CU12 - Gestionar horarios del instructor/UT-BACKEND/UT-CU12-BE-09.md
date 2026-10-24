# UT-CU12-BE-09 — `FranjaService`: listar, bloquear y desbloquear

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-09` |
| **Nombre** | `FranjaService`: listar, bloquear y desbloquear |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-03`, `UT-CU12-BE-04`, `UT-CU12-BE-05`, `UT-CU12-BE-06` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor necesita ver sus franjas de un día y sacar de circulación una que no puede atender (y devolverla cuando pueda).

## Objetivo

`FranjaService` lista las franjas del instructor y cambia una entre `LIBRE` y `BLOQUEADA` con las reglas del CU.

## Alcance

- `features/horarios/service/FranjaService` (clase concreta, `@RequiredArgsConstructor`; usa `FranjaRepository`, `Clock`). Los tres métodos con `@PreAuthorize("hasRole('INSTRUCTOR')")`.
- **`listar(UUID instructorCodigo, LocalDate fecha, EstadoFranja estado, int page, int pageSize)` → página de `FranjaResponse`** + total, ordenada por `fecha` y `horaInicio`. Sin `fecha`, devuelve desde hoy en adelante (hoy según el `Clock`). Mismo tope de `pageSize` que el listado de horarios (UT 07). Sin log.
- **`bloquear(UUID instructorCodigo, UUID franjaCodigo)` → `FranjaResponse`**, en este orden de comprobación:
  1. No existe o es de otro instructor → `NotFoundException` `ERR_ACA_07`.
  2. La franja ya empezó (`fecha` + `horaInicio` no posteriores a la hora actual) → `ConflictException` `ERR_ACA_10`.
  3. Cambia el estado con `cambiarEstado(..., LIBRE, BLOQUEADA)` (atómico). Si no estaba `LIBRE` (reservada, ya bloqueada, o cambió entre la lectura y la escritura) → `ConflictException` `ERR_ACA_08`.
  4. Log `INFO` (`module="horarios"`, `instructorCodigo`, `franjaCodigo`, `estado`).
- **`desbloquear(UUID instructorCodigo, UUID franjaCodigo)`:** igual con `BLOQUEADA` → `LIBRE` y `ERR_ACA_09` en lugar de `ERR_ACA_08`.
- Tests unitarios (repositorio mockeado, `Clock` fijo): bloquea una libre futura y devuelve la franja ya bloqueada; desbloquea una bloqueada futura; reservada → `ERR_ACA_08` sin cambios; ya bloqueada al bloquear → `ERR_ACA_08`; libre al desbloquear → `ERR_ACA_09`; inexistente y de otro instructor → `ERR_ACA_07`; ya empezó (incluida la que empieza exactamente ahora) → `ERR_ACA_10`; si `cambiarEstado` devuelve vacío por una carrera → el error de estado correspondiente; el listado pasa `fecha` o "desde hoy", `estado` y el orden; sin `fecha` usa hoy.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/FranjaServiceTest.java`

## Fuera de alcance (no tocar)

- Pasar una franja a `RESERVADA` ni liberarla: es de CU14 y CU15.
- Consultas de franjas para el estudiante o la escuela (CU11, CU13, CU16).
- El controller (UT 11).

## Resultado esperado

El instructor puede listar sus franjas, bloquear las libres y desbloquear las bloqueadas, y cualquier otro caso responde con el error catalogado sin cambiar nada.

## Validación

1. `./mvnw test` en `academico` pasa con `FranjaServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
