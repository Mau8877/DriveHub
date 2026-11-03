# UT-CU17-BE-07 — `ClaseService`: ver y corregir una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-07` |
| **Nombre** | `ClaseService`: ver y corregir una clase |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-02`, `UT-CU17-BE-04`, `UT-CU17-BE-06` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#put-vs-patch) (`PATCH` parcial: lo que no se envía no se toca), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio). Reutiliza `ClaseService` y la resolución del nombre del estudiante de la UT 06. Decisiones 8 y 10 del [README](./README.md).

## Solicitud de negocio

El instructor que se equivocó al escribir los kilómetros o las observaciones quiere ver lo guardado y corregirlo sin tener que cerrar la clase otra vez.

## Objetivo

`ClaseService` permite ver una clase cerrada propia y corregir su tema, observaciones y kilómetros.

## Alcance

- `ClaseService` (se edita la clase de la UT 06, sin tocar `cerrar`):
  - `@PreAuthorize("hasRole('INSTRUCTOR')") ClaseResponse obtener(UUID instructorCodigo, UUID escuelaCodigo, String bearer, UUID codigoClase)`: escuela nula → `ERR_ACA_16`; busca con `findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull` y si no hay → `NotFoundException` `ERR_ACA_35`; arma la `ClaseResponse` con el nombre del estudiante (mismo respaldo `Estudiante`, **sin error** si Identidad falla). No loguea (es una lectura).
  - `@PreAuthorize("hasRole('INSTRUCTOR')") ClaseResponse actualizar(UUID instructorCodigo, UUID escuelaCodigo, String bearer, UUID codigoClase, ActualizarClaseRequest request)`:
    0. Escuela nula → `ERR_ACA_16`.
    1. Busca la clase como en `obtener` (`ERR_ACA_35`).
    2. `ReglasDeClase.validarCorreccion(clase, request)` (`ERR_ACA_38`), antes de cambiar nada.
    3. Aplica **solo lo enviado**: `tema` y `observaciones` no nulos se limpian (`ReglasDeClase.limpiar`, un blanco las borra); `kmInicio` y `kmFin` no nulos reemplazan lo guardado. Lo que viene `null` no se toca. Guarda.
    4. Arma la respuesta como en `obtener`.
  - Un cuerpo sin ningún campo no cambia nada y no guarda (no hay escritura inútil), pero responde la clase igual.
  - La asistencia, la reserva, la fecha y el estado no se pueden cambiar por aquí (no están en el request).
  - Log: `INFO` «Clase corregida» con `claseCodigo` e `instructorCodigo` y la lista de **campos** cambiados (`tema`, `kmFin`...), nunca sus valores.
- Tests de servicio (repositorio, `IdentidadClient` simulados): `obtener` devuelve la clase propia; inexistente, borrada o de otro instructor → `ERR_ACA_35`; Identidad caída → `Estudiante` y sin excepción; `actualizar` cambia solo el tema y deja lo demás; cambia solo un kilometraje y lo valida contra el otro ya guardado (`kmFin` menor que el `kmInicio` guardado → `ERR_ACA_38` y no guarda); un blanco borra el texto; `null` no lo borra; con una clase `AUSENTE`, tema o km → `ERR_ACA_38` y solo observaciones pasa; cuerpo vacío no guarda; clase ajena → `ERR_ACA_35`; no se puede cambiar la asistencia (comprobado por el tipo del request); el log lista los campos y no incluye los valores; `@PreAuthorize("hasRole('INSTRUCTOR')")` en `obtener` y `actualizar` (por reflexión); escuela nula → `ERR_ACA_16`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/ClaseService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/ClaseServiceTest.java`

## Fuera de alcance (no tocar)

- Cerrar una clase (UT 06) y la agenda (UT 08).
- Cambiar la asistencia, reabrir la clase o borrar un kilometraje.
- El controller y la ruta (UT 09).

## Resultado esperado

El instructor puede ver una clase cerrada suya y corregirle el tema, las observaciones y los kilómetros, con las mismas reglas que al cerrar.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
