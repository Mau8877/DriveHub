# UT-CU09-BE-05 — Editar la ubicación de un instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-05` |
| **Nombre** | Editar la ubicación de un instructor |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU09-BE-02, UT-CU09-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `EditarEstudianteService` y `EditarPerfilService`.

## Solicitud de negocio

La escuela puede corregir dónde vive un instructor; la dirección la calcula el servidor desde el punto del mapa, y el resto de sus datos personales no se toca.

## Objetivo

Existe el caso de uso que cambia únicamente la ubicación (coordenadas y dirección) de un instructor de la escuela de quien llama.

## Alcance

- `domain/port/in/EditarUbicacionInstructorUseCase` (`PersonaInstructor editar(UUID administradorCodigo, UUID instructorCodigo, BigDecimal latitud, BigDecimal longitud)`) y `EditarUbicacionInstructorService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`:
  - Toma la escuela de la cuenta del Administrador (`EscuelaDelAdministrador`) y busca al instructor con `buscarInstructorDeEscuela` → `NotFoundException` `ERR_IDE_22` si no existe, no es instructor o es de otra escuela.
  - Latitud y longitud son obligatorias y van juntas (`ValidationException` `ERR_SYS_01` si falta alguna).
  - Si las coordenadas son distintas de las guardadas, recalcula la dirección con `GeocodificadorInversoPort` (vacía = `null` con `WARN`, sin error); si son iguales no consulta el mapa.
  - Modifica **solo** `latitud`, `longitud` y `direccion` del `Perfil` y guarda con `actualizar`; devuelve la persona con `PersonaInstructorMapeador`.
  - `INFO` "Ubicación de instructor actualizada" con `usuarioCodigo` y `administradorCodigo`, sin coordenadas.
- `IdentidadErrorCodes.INSTRUCTOR_NO_ENCONTRADO` = `ERR_IDE_22`. Documentarlo en `docs/errors/identidad/ERRORES_IDENTIDAD.md` (404, `NotFoundException`, "El instructor no existe en la escuela", `EditarUbicacionInstructorService.editar()`) y enlazarlo en `docs/errors/README.md`.
- Pruebas unitarias con los puertos simulados: cambia el punto y recalcula la dirección; el mismo punto no llama al geocodificador; el geocodificador sin respuesta deja la dirección en blanco; `ERR_IDE_22` (inexistente, de otra escuela y estudiante); una sola coordenada → `ERR_SYS_01`; los datos personales y el acceso del `Perfil`/`Usuario` guardado no cambian. La autorización por rol **no** se prueba acá: se ejerce en la UT 06.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/EditarUbicacionInstructorUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/EditarUbicacionInstructorService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- Tests del servicio
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint (UT 06).
- Cambiar cualquier otro dato de la persona (es de su propio perfil, CU06) o su estado de cuenta.
- La ficha profesional: es de Escuela.

## Resultado esperado

Identidad puede corregir la ubicación de un instructor de la escuela y nada más.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
