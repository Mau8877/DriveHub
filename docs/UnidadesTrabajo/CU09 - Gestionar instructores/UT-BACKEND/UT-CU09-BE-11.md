# UT-CU09-BE-11 — Editar un instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-11` |
| **Nombre** | Editar un instructor |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU09-BE-07, UT-CU09-BE-08, UT-CU09-BE-09 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`PATCH` como edición parcial), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `EditarEscuelaUseCase` y, en Identidad, `EditarEstudianteService`.

## Solicitud de negocio

La escuela mantiene al día la licencia, las categorías, el estado y la ubicación de sus instructores; nada de sus datos personales.

## Objetivo

Existe el caso de uso que cambia únicamente la ficha profesional y la ubicación de un instructor de la escuela.

## Alcance

- `application/dto/EditarInstructorInput(escuelaCodigo, instructorCodigo, nroLicencia, licenciaVence, fechaContratacion, categorias, estado, latitud, longitud)`: todos los campos de la ficha y de la ubicación son opcionales (`null` = no tocar). No hay campo para ningún dato personal a propósito.
- `application/usecase/EditarInstructorUseCase` (`ejecutar(input)`) con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`:
  1. `buscarPorCodigo(escuela, codigo)` → `ERR_ESC_09` si no existe o es de otra escuela.
  2. Si no llega ningún campo → `ValidationException` `ERR_SYS_01` ("No hay nada que cambiar"); si llega una sola coordenada → `ERR_SYS_01`.
  3. Valida **solo lo que llega**: número de licencia normalizado y con formato válido y no usado por **otro** instructor (`existePorNroLicenciaDeOtro`) → `ERR_ESC_08`; **vencimiento posterior a hoy solo si es distinto del guardado** (dejar sin tocar una licencia ya vencida no impide corregir otros datos); contratación no futura; categorías con las mismas reglas del alta (`ERR_ESC_10` si alguna no existe o es `SIN`, `ERR_SYS_01` si llega vacío o con repetidas); estado de `ACTIVO`, `LICENCIA` o `INACTIVO`.
  4. Si hay coordenadas distintas de las guardadas, llama a `InstructorCuentaGateway.cambiarUbicacion`. **Antes** de guardar la ficha: si esa llamada falla, no se cambió nada de la ficha; si la ficha fallara después, la ubicación nueva queda (inofensivo, el reintento la repite).
  5. Guarda la ficha con `actualizar` (reemplaza las categorías) y devuelve el detalle con `InstructorMapeador`.
  6. Registra `INFO` "Instructor actualizado" con `instructorCodigo`, `escuelaCodigo` y los **nombres** de los campos modificados, sin valores.
- Pruebas unitarias con los puertos simulados: cambia cada campo por separado y todos juntos; las categorías se reemplazan; `ERR_ESC_09` (inexistente y de otra escuela); cuerpo sin campos y una sola coordenada → `ERR_SYS_01`; licencia de otro instructor → `ERR_ESC_08` y la propia no choca; licencia vencida sin tocar + otro cambio → pasa; licencia vencida cambiada a otra vencida → `ERR_SYS_01`; categoría inexistente, `SIN`, vacía → error sin guardar ni llamar a Identidad; el mismo punto no llama a Identidad; si Identidad falla no se guarda la ficha; el `usuarioCodigo`, la escuela y los datos personales no cambian. La autorización por rol **no** se prueba acá: se ejerce en la UT 12.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/EditarInstructorInput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/EditarInstructorUseCase.java`
- Tests del caso de uso
- `docs/errors/escuela/ERRORES_ESCUELA.md` (nuevos lugares donde se lanzan `ERR_ESC_08`, `ERR_ESC_09` y `ERR_ESC_10`)

## Fuera de alcance (no tocar)

- El endpoint (UT 12).
- Cambiar nombres, CI, sexo, fecha de nacimiento, teléfono, correo, contraseña o escuela.
- Que el estado bloquee la sesión o las asignaciones.
- Borrar al instructor.

## Resultado esperado

El Administrador puede corregir la ficha profesional y la ubicación de un instructor y nada más.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
